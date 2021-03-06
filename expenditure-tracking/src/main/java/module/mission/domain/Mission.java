/*
 * @(#)Mission.java
 *
 * Copyright 2010 Instituto Superior Tecnico
 * Founding Authors: Luis Cruz, Nuno Ochoa, Paulo Abrantes
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the Expenditure Tracking Module.
 *
 *   The Expenditure Tracking Module is free software: you can
 *   redistribute it and/or modify it under the terms of the GNU Lesser General
 *   Public License as published by the Free Software Foundation, either version
 *   3 of the License, or (at your option) any later version.
 *
 *   The Expenditure Tracking Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Expenditure Tracking Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package module.mission.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import module.finance.util.Money;
import module.geography.domain.Country;
import module.mission.domain.activity.DistributeItemCostsActivityInformation;
import module.mission.domain.activity.DistributeItemCostsActivityInformation.MissionItemFinancerBean;
import module.mission.domain.activity.DistributeItemCostsActivityInformation.MissionItemFinancerBeanCollection;
import module.mission.domain.activity.ItemActivityInformation;
import module.mission.domain.activity.UpdateMissionDetailsActivityInformation;
import module.mission.domain.util.AuthorizationChain;
import module.mission.domain.util.ParticipantAuthorizationChain;
import module.organization.domain.Accountability;
import module.organization.domain.AccountabilityType;
import module.organization.domain.Party;
import module.organization.domain.Person;
import module.workflow.domain.WorkflowLog;
import module.workflow.domain.WorkflowQueue;
import pt.ist.expenditureTrackingSystem._development.Bundle;
import pt.ist.expenditureTrackingSystem.domain.ExpenditureTrackingSystem;
import pt.ist.expenditureTrackingSystem.domain.authorizations.Authorization;
import pt.ist.expenditureTrackingSystem.domain.organization.AccountingUnit;
import pt.ist.expenditureTrackingSystem.domain.organization.Unit;
import pt.ist.expenditureTrackingSystem.domain.util.DomainException;

/**
 * 
 * @author Luis Cruz
 * 
 */
public abstract class Mission extends Mission_Base {

    public static final Comparator<Mission> COMPARATOR_BY_PROCESS_IDENTIFICATION = new Comparator<Mission>() {

        @Override
        public int compare(Mission o1, Mission o2) {
            final MissionProcess p1 = o1.getMissionProcess();
            final MissionProcess p2 = o2.getMissionProcess();

            return MissionProcess.COMPARATOR_BY_PROCESS_NUMBER.compare(p1, p2);
        }

    };

    public Mission() {
        super();
        setMissionSystem(MissionSystem.getInstance());
        setIsApprovedByMissionResponsible(Boolean.FALSE);
        final User user = Authenticate.getUser();
        final Person person = user == null ? null : user.getPerson();
        setRequestingPerson(person);
        new MissionVersion(this);
        setIsPersonalInformationProcessed(false);
        setIsVerified(false);
    }

    protected void setMissionInformation(final String location, final DateTime daparture, final DateTime arrival,
            final String objective, final Boolean isCurrentUserAParticipant, final Boolean grantOwnerEquivalence) {
        final User user = Authenticate.getUser();
        final Person person = user == null ? null : user.getPerson();

        setMissionInformation(location, daparture, arrival, objective);

        if (isCurrentUserAParticipant != null && isCurrentUserAParticipant.booleanValue()) {
            if (person != null) {
                addParticipantes(person);
            }
        }
        setGrantOwnerEquivalence(grantOwnerEquivalence);
    }

    protected void setMissionInformation(final String location, final DateTime daparture, final DateTime arrival,
            final String objective) {
        setLocation(location);

        if (daparture == null || arrival == null || arrival.isBefore(daparture)) {
            throw new DomainException(Bundle.MISSION, "label.mission.process.invalid.dates");
        }

        final MissionVersion missionVersion = getMissionVersion();
        missionVersion.setDates(daparture, arrival);

        setObjective(objective);
    }

    public Set<MissionFinancer> getFinancer() {
        return getMissionVersion().getFinancer();
    }

    public void fill(final UpdateMissionDetailsActivityInformation updateMissionDetailsActivityInformation) {
        updateMissionDetailsActivityInformation.setLocation(getLocation());
        updateMissionDetailsActivityInformation.setDaparture(getDaparture());
        updateMissionDetailsActivityInformation.setArrival(getArrival());
        updateMissionDetailsActivityInformation.setObjective(getObjective());
    }

    @Override
    public DateTime getDaparture() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getDaparture();
    }

    @Override
    public DateTime getArrival() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getArrival();
    }

    public void updateDetails(final UpdateMissionDetailsActivityInformation updateMissionDetailsActivityInformation) {
        setMissionInformation(updateMissionDetailsActivityInformation.getLocation(),
                updateMissionDetailsActivityInformation.getDaparture(), updateMissionDetailsActivityInformation.getArrival(),
                updateMissionDetailsActivityInformation.getObjective());
    }

    public void add(final ItemActivityInformation itemActivityInformation) {
        final MissionItem missionItem = itemActivityInformation.getMissionItem();
        missionItem.setMissionVersion(getMissionVersion());

        final Collection<Person> people = itemActivityInformation.getPeople();
        final Set<Person> participants = missionItem.getPeopleSet();
        participants.addAll(people);
        participants.retainAll(people);
        if (getParticipantesSet().size() == 1) {
            participants.addAll(getParticipantesSet());
        }

//	final MissionItemFinancerBeanCollection missionItemFinancerBeanCollection = itemActivityInformation.getMissionItemFinancerBeans();
//	distributeCosts(missionItem, missionItemFinancerBeanCollection);
    }

    public void distributeCosts(final DistributeItemCostsActivityInformation distributeItemCostsActivityInformation) {
        final MissionItem missionItem = distributeItemCostsActivityInformation.getMissionItem();
        final MissionItemFinancerBeanCollection missionItemFinancerBeanCollection =
                distributeItemCostsActivityInformation.getMissionItemFinancerBeans();
        distributeCosts(missionItem, missionItemFinancerBeanCollection);
    }

    public void distributeCosts(final MissionItem missionItem,
            final MissionItemFinancerBeanCollection missionItemFinancerBeanCollection) {
        if (missionItemFinancerBeanCollection.size() == 1) {
            final MissionItemFinancerBean missionItemFinancerBean = missionItemFinancerBeanCollection.iterator().next();
            missionItemFinancerBean.setAmount(missionItem.getValue());
        }
        missionItem.setMissionItemFinancers(missionItemFinancerBeanCollection);
    }

    public void addFinancer(final Unit unit) {
        new MissionFinancer(this, unit);
    }

    public boolean hasAnyAproval() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.getApproval() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isPendingApproval() {
        return getApprovalForMissionWithNoFinancers() == null
                && (getIsApprovedByMissionResponsible() == null || !getIsApprovedByMissionResponsible().booleanValue());
    }

    public boolean isPendingApprovalBy(final User user) {
        return isPendingApproval() && isMissionResponsible(user);
    }

    private boolean isMissionResponsible(final User user) {
        final Party missionResponsible = getMissionResponsible();
        return (missionResponsible != null && missionResponsible.isPerson() && missionResponsible == user.getPerson())
                || (missionResponsible != null && missionResponsible.isUnit()
                        && getExpenditureUnit((module.organization.domain.Unit) missionResponsible)
                                .isResponsible(user.getExpenditurePerson()));
    }

    private Unit getExpenditureUnit(final module.organization.domain.Unit unit) {
        return unit.getExpenditureUnit() != null ? unit.getExpenditureUnit() : getExpenditureUnit(
                unit.getParentAccountabilityStream().filter(a -> a.getParent().isUnit())
                        .map(a -> (module.organization.domain.Unit) a.getParent()).findAny().orElse(null));
    }

    public boolean isPendingAuthorizationBy(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.isPendingAuthorizationBy(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBeenCheckedByUnderlings() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (!financer.hasBeenCheckedByUnderlings()) {
                return false;
            }
        }
        return true;
    }

    public boolean isPendingDirectAuthorizationBy(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.isPendingDirectAuthorizationBy(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPendingCheckByUnderlings(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.isPendingCheckByUnderlings(user)) {
                return true;
            }
        }
        return false;
    }

    public void approve(final User user) {
        final Party missionResponsible = getMissionResponsible();
        if (missionResponsible.isPerson()) {
            setIsApprovedByMissionResponsible(Boolean.TRUE);
        } else if (missionResponsible.isUnit()) {
            final Unit unit = getExpenditureUnit((module.organization.domain.Unit) missionResponsible);
            final Authorization authorization = unit.findClosestAuthorization(user.getExpenditurePerson(), Money.ZERO);
            if (authorization != null) {
                setApprovalForMissionWithNoFinancers(authorization);
            }
        }

        setServiceGaranteePerson(user.getPerson());
        setServiceGaranteeInstante(new DateTime());
    }

    public void authorize(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.authorize(user);
        }
    }

    public void preAuthorize(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.preAuthorize(user);
        }
    }

    public boolean canRemoveApproval(final User user) {
        if (getFinancerCount() == 0 && getMissionItemsCount() == 0) {
            final Party missionResponsible = getMissionResponsible();
            if (missionResponsible == null) {
                return false;
            }
            if (missionResponsible.isPerson()) {
                return getIsApprovedByMissionResponsible() != null && getIsApprovedByMissionResponsible().booleanValue()
                        && missionResponsible == user.getPerson();
            } else if (missionResponsible.isUnit()) {
                final Unit unit = getExpenditureUnit((module.organization.domain.Unit) missionResponsible);
                return getApprovalForMissionWithNoFinancers() != null && unit.isResponsible(user.getExpenditurePerson());
            }
        }
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.canRemoveApproval(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean canRemoveAuthorization(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.canRemoveAuthorization(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean canRemovePreAuthorization(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.canRemovePreAuthorization(user)) {
                return true;
            }
        }
        return false;
    }

    public void unapprove(final User user) {
        if (getFinancerCount() == 0 && getMissionItemsCount() == 0) {
            final Party missionResponsible = getMissionResponsible();
            if (missionResponsible.isPerson()) {
                setIsApprovedByMissionResponsible(Boolean.FALSE);
            } else if (missionResponsible.isUnit()) {
                final Unit unit = getExpenditureUnit((module.organization.domain.Unit) missionResponsible);
                if (unit.isResponsible(user.getExpenditurePerson())) {
                    setApprovalForMissionWithNoFinancers(null);
                }
            }
        } else {
            for (final MissionFinancer financer : getFinancerSet()) {
                financer.unapprove(user);
            }
        }
    }

    public void unauthorize(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.unauthorize(user);
        }
    }

    public void unPreAuthorize(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.unPreAuthorize(user);
        }
    }

    public boolean isApprovedByResponsible() {
        return getIsApprovedByMissionResponsible() != null && getIsApprovedByMissionResponsible();
    }

    public boolean isApproved() {
        return isApprovedByResponsible() && isVerified() && (areAllVehicleItemsAuthorized() || !isVehicleAuthorizationNeeded());
    }

    public boolean isVerified() {
        return getIsVerified();
    }

    protected boolean isVehicleAuthorizationNeeded() {
        return !areAllParticipantsAuthorized();
    }

    public boolean isPersonalInformationProcessed() {
        return getIsPersonalInformationProcessed();
    }

    public boolean areAllVehicleItemsAuthorized() {
        for (VehiclItem item : getVehicleItems()) {
            if (!item.isAuthorized()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Use getVehicleItemStream instead
     * 
     * @return
     */
    @Deprecated
    public List<VehiclItem> getVehicleItems() {
        return getMissionVersion().getVehicleItems();
    }

    public Stream<VehiclItem> getVehicleItemStream() {
        return getMissionVersion().getVehicleItemStream();
    }

    public boolean hasAllAllocatedFunds() {
        if (!hasAnyFinancer()) {
            return false;
        }
        for (final MissionFinancer financer : getFinancerSet()) {
            if (!financer.hasAllAllocatedFunds()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAllAllocatedProjectFunds() {
        if (!hasAnyProjectFinancer()) {
            return false;
        }
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.isProjectFinancer() && !financer.hasAllAllocatedProjectFunds()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAnyProjectFinancer() {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            for (final MissionItemFinancer missionItemFinancer : missionFinancer.getMissionItemFinancersSet()) {
                if (missionItemFinancer instanceof MissionItemProjectFinancer) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAnyAllocatedFunds() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.hasAnyAllocatedFunds()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyAllocatedProjectFunds() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.hasAnyAllocatedProjectFunds()) {
                return true;
            }
        }
        return false;
    }

    public void unAllocateFunds(Person person) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.unAllocateFunds(person);
        }
    }

    public boolean hasAnyAuthorization() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.getAuthorization() != null) {
                return true;
            }
        }
        return false;
    }

    protected Set<MissionItem> getItemsByType(final Class clazz) {
        final Set<MissionItem> missionItems = new TreeSet<MissionItem>(MissionItem.COMPARATOR_BY_OID);
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (clazz.isAssignableFrom(missionItem.getClass())) {
                missionItems.add(missionItem);
            }
        }
        return missionItems;
    }

    public Set<MissionItem> getTransportationItems() {
        return getItemsByType(TransportationItem.class);
    }

    public Set<MissionItem> getOtherItems() {
        return getItemsByType(OtherMissionItem.class);
    }

    public Set<MissionItem> getPersonelExpenseItems() {
        return getItemsByType(PersonelExpenseItem.class);
    }

    public Set<MissionItem> getAccommodationItems() {
        return getItemsByType(AccommodationItem.class);
    }

    public SortedSet<Person> getOrderedParticipants() {
        final SortedSet<Person> people = new TreeSet<Person>(PERSON_COMPARATOR_BY_NAME);
        people.addAll(getParticipantesSet());
        return people;
    }

    @Override
    public void removeParticipantes(final Person participante) {
        super.removeParticipantes(participante);
        for (final MissionItem missionItem : getMissionItemsSet()) {
            missionItem.removePeople(participante);
        }
        if (getMissionResponsible() == participante) {
            if (!getParticipantesSet().isEmpty()) {
                setMissionResponsible(getParticipantesSet().iterator().next());
            } else {
                setMissionResponsible(null);
            }
        }

        if (getParticipantesSet().size() == 1) {
            final Person person = (Person) getMissionResponsible();
            for (final MissionItem missionItem : getMissionItemsSet()) {
                missionItem.addPeople(person);
            }
        }
    }

    public static Comparator<UserProfile> PROFILE_COMPARATOR_BY_NAME = Comparator.nullsLast((up1, up2) -> {
        final String n1 = up1.getFamilyNames();
        final String n2 = up2.getFamilyNames();
        return n1 != null && n2 != null ? n1.compareTo(n2) : n1 == null && n2 == null ? 0 : n1 == null ? 1 : -1;
    });

    public static Comparator<Person> PERSON_COMPARATOR_BY_NAME = Comparator.nullsLast((p1, p2) -> {
        final User u1 = p1 == null ? null : p1.getUser();
        final User u2 = p2 == null ? null : p2.getUser();
        final UserProfile up1 = u1 == null ? null : u1.getProfile();
        final UserProfile up2 = u2 == null ? null : u2.getProfile();
        return PROFILE_COMPARATOR_BY_NAME.compare(up1, up2);
    });

    public static Comparator<Person> PERSON_COMPARATOR_BY_FULLNAME = Comparator.nullsLast((p1, p2) -> {
        String sp1 = p1 == null ? null : p1.getPresentationName();
        String sp2 = p2 == null ? null : p2.getPresentationName();
        return sp1 != null && sp2 != null ? sp1.compareTo(sp2) : sp1 == null && sp2 == null ? 0 : sp1 == null ? 1 : -1;
    });

    public SortedMap<Person, PersonMissionAuthorization> getParticipantAuthorizations() {
        final SortedMap<Person, PersonMissionAuthorization> participantAuthorizations =
                new TreeMap<Person, PersonMissionAuthorization>(PERSON_COMPARATOR_BY_FULLNAME);
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.getPrevious() == null) {
                participantAuthorizations.put(personMissionAuthorization.getSubject(), personMissionAuthorization);
            }
        }
        for (final Person person : getParticipantesSet()) {
            if (!participantAuthorizations.containsKey(person)) {
                participantAuthorizations.put(person, null);
            }
        }
        return participantAuthorizations;
    }

    public SortedMap<Person, Collection<ParticipantAuthorizationChain>> getPossibleParticipantAuthorizationChains() {
        final SortedMap<Person, Collection<ParticipantAuthorizationChain>> participantAuthorizationChainss =
                new TreeMap<Person, Collection<ParticipantAuthorizationChain>>(PERSON_COMPARATOR_BY_NAME);
        for (final Person person : getParticipantesSet()) {
            final Collection<ParticipantAuthorizationChain> participantAuthorizationChain =
                    ParticipantAuthorizationChain.getParticipantAuthorizationChains(person);
            participantAuthorizationChainss.put(person, participantAuthorizationChain);
        }
        return participantAuthorizationChainss;
    }

    abstract public Country getCountry();

    @Override
    public void addParticipantes(final Person person) {
        super.addParticipantes(person);
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.getSubject() == person) {
                return;
            }
        }
        final ParticipantAuthorizationChain participantAuthorizationChain =
                ParticipantAuthorizationChain.getMostLikelyParticipantAuthorizationChain(person);
        if (participantAuthorizationChain != null) {
            final AuthorizationChain authorizationChain = participantAuthorizationChain.getAuthorizationChain();
            new PersonMissionAuthorization(this, person, authorizationChain);
        }
        if (!hasMissionResponsible()) {
            setMissionResponsible(person);
        }
    }

    public boolean areAllPrevisionaryCostsAreDistributed() {
        final int numberFinancers = getFinancerCount();
        final Money missionCosts = calculatePrevisionaryCosts();
        final Money distributedCosts = calculateDistributedCosts();
        return (numberFinancers == 0 && missionCosts.isZero())
                || (!missionCosts.isZero() && missionCosts.equals(distributedCosts));
    }

    private Money calculatePrevisionaryCosts() {
        Money result = Money.ZERO;
        for (final MissionItem missionItem : getMissionItemsSet()) {
            result = result.add(missionItem.getPrevisionaryCosts());
        }
        return result;
    }

    private Money calculateDistributedCosts() {
        Money result = Money.ZERO;
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            result = result.add(missionFinancer.getAmount());
        }
        return result;
    }

    public boolean areAllParticipantAuthorizationChainsDefined() {
        if (getParticipantesSet().isEmpty()) {
            return false;
        }
        for (final Person person : getParticipantesSet()) {
            final PersonMissionAuthorization personMissionAuthorization = getPersonMissionAuthorization(person);
            if (personMissionAuthorization == null) {
                return false;
            }
        }
        return true;
    }

    protected PersonMissionAuthorization getPersonMissionAuthorization(final Person person) {
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.getSubject() == person) {
                return personMissionAuthorization;
            }
        }
        return null;
    }

    public boolean canAuthoriseParticipantActivity() {
        final User currentUser = Authenticate.getUser();
        if (currentUser == null || currentUser.getPerson() == null) {
            return false;
        }

        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.canAuthoriseParticipantActivity(currentUser.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean canUnAuthoriseParticipantActivity() {
        final User currentUser = Authenticate.getUser();
        if (currentUser == null || currentUser.getPerson() == null) {
            return false;
        }

        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.canUnAuthoriseParticipantActivity(currentUser.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean canUnAuthoriseSomeParticipantActivity() {
        final User currentUser = Authenticate.getUser();
        if (currentUser == null || currentUser.getPerson() == null) {
            return false;
        }

        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.canUnAuthoriseSomeParticipantActivity(currentUser.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean canAllocateFund() {
        if (!hasAnyFinancer()) {
            return false;
        }
        final User user = Authenticate.getUser();
        if (user == null) {
            return false;
        }

        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.canAllocateFunds(user.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean canAllocateProjectFund() {
        if (!hasAnyProjectFinancer()) {
            return false;
        }
        final User user = Authenticate.getUser();
        if (user == null) {
            return false;
        }

        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.hasAnyMissionItemProjectFinancers() && financer.canAllocateProjectFunds(user.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean isDirectResponsibleForPendingProjectFundAllocation() {
        if (!hasAnyProjectFinancer()) {
            return false;
        }
        final User user = Authenticate.getUser();
        if (user == null) {
            return false;
        }

        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.hasAnyMissionItemProjectFinancers()
                    && financer.isDirectResponsibleForPendingProjectFundAllocation(user.getPerson())) {
                return true;
            }
        }
        return false;
    }

    public boolean getHasVehicleItem() {
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (missionItem.isVehicleItem()) {
                return true;
            }
        }
        return false;
    }

    public Money getValue() {
        Money result = Money.ZERO;
        for (final MissionItem missionItem : getMissionItemsSet()) {
            result = result.add(missionItem.getValue());
        }
        return result;
    }

    public boolean hasAnyAuthorizedParticipants() {
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.hasAnyAuthorization()) {
                return true;
            }
        }
        return false;
    }

    public Collection<DailyPersonelExpenseCategory> getPossibleDailyPersonalExpenseCategories() {
        final DailyPersonelExpenseTable dailyPersonelExpenseTable = getDailyPersonelExpenseTable();
        return dailyPersonelExpenseTable == null ? Collections.EMPTY_SET : dailyPersonelExpenseTable
                .getSortedDailyPersonelExpenseCategories();
    }

    public double getFirstDayPersonelDayExpensePercentage(final PersonelExpenseItem personelExpenseItem) {
        return 1;
    }

    public double getMiddleDayPersonelDayExpensePercentage(final PersonelExpenseItem personelExpenseItem) {
        return 1;
    }

    public double getLastDayPersonelDayExpensePercentage(final PersonelExpenseItem personelExpenseItem) {
        return 1;
    }

    public void unAllocateProjectFunds(Person person) {
        for (final MissionFinancer financer : getFinancerSet()) {
            financer.unAllocateProjectFunds(person);
        }
    }

    public boolean isConsistent() {
        if (!hasMissionResponsible()) {
            return false;
        }

        // Check mission items individually
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (!missionItem.isConsistent()) {
                return false;
            }
        }

        final int numberOfMissionDays = calculateNumberOfNights() + 1;

        // Cross-check personel expenses
        for (final Person person : getParticipantesSet()) {
            final int numberOfPersonelExpenseDays = PersonelExpenseItem.calculateNumberOfFullPersonelExpenseDays(this, person);
            if (numberOfMissionDays < numberOfPersonelExpenseDays) {
                return false;
            }
        }

        // Cross-check accomodations and personel expenses
        for (final Person person : getParticipantesSet()) {
            final int numberOfFullPersonelExpenseDays =
                    FullPersonelExpenseItem.calculateNumberOfFullPersonelExpenseDays(this, person);
            final int numberOfAccomodatedNights = AccommodationItem.calculateNumberOfAccomodatedNights(this, person);
            if (numberOfMissionDays < numberOfFullPersonelExpenseDays + numberOfAccomodatedNights) {
                return false;
            }
        }

        // Check all financers have an accounting unit and some value attributed
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            if (missionFinancer.getUnit() == null || missionFinancer.getAccountingUnit() == null
                    || missionFinancer.getAmount().isZero()) {
                return false;
            }
        }

        // Other basic checks
        return !getParticipantesSet().isEmpty() && areAllParticipantAuthorizationChainsDefined()
                && areAllPrevisionaryCostsAreDistributed();
    }

    public Collection<String> getConsistencyMessages() {
        final Collection<String> result = new TreeSet<String>();

        if (getParticipantesSet().isEmpty()) {
            // !getParticipantesSet().isEmpty()
            result.add(BundleUtil.getString("resources/MissionResources", "message.mission.participants.empty"));
        } else {
            // areAllParticipantAuthorizationChainsDefined()
            for (final Person person : getParticipantesSet()) {
                final PersonMissionAuthorization personMissionAuthorization = getPersonMissionAuthorization(person);
                if (personMissionAuthorization == null) {
                    result.add(BundleUtil.getString("resources/MissionResources",
                            "message.mission.participant.authorization.chain.not.defined",
                            person.getUser().getProfile().getFullName()));
                }
                if (!hasAnyCurrentRelationToInstitution(person) && hasAnyPersonelExpenseItems(person)) {
                    result.add(BundleUtil.getString("resources/MissionResources",
                            "message.mission.participant.with.no.relation.to.institution.has.personel.expense.items",
                            person.getUser().getProfile().getFullName()));
                }
            }
        }

        // areAllPrevisionaryCostsAreDistributed()
        final int numberFinancers = getFinancerCount();
        final Money missionCosts = calculatePrevisionaryCosts();
        final Money distributedCosts = calculateDistributedCosts();
        if (numberFinancers > 0 && missionCosts.isZero()) {
            result.add(BundleUtil.getString("resources/MissionResources", "message.mission.with.financers.and.no.costs"));
        } else if (!missionCosts.isZero() && !missionCosts.equals(distributedCosts)) {
            result.add(BundleUtil.getString("resources/MissionResources", "message.mission.costs.not.distributed"));
        }

        // Check mission items individually
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (!missionItem.isConsistent()) {
                if (missionItem instanceof OtherPersonelExpenseItem
                        && hasValidDates((OtherPersonelExpenseItem) missionItem)
                        && !((OtherPersonelExpenseItem) missionItem).doesNotExceedMaximumPossiblePersonelExpenseValue()) {
                    result.add(BundleUtil.getString("resources/MissionResources",
                            "message.mission.item.other.personel.expense.exceeds.max.value", missionItem.getLocalizedName()));
                } else {
                    result.add(BundleUtil.getString("resources/MissionResources", "message.mission.item.not.consistent",
                            missionItem.getLocalizedName()));
                }
            }
        }

        // Cross-check personel expenses
        final int numberOfMissionDays = calculateNumberOfNights() + 1;
        for (final Person person : getParticipantesSet()) {
            final int numberOfPersonelExpenseDays = PersonelExpenseItem.calculateNumberOfFullPersonelExpenseDays(this, person);
            if (numberOfMissionDays < numberOfPersonelExpenseDays) {
                result.add(BundleUtil.getString("resources/MissionResources", "message.mission.personel.expense.days.not.match",
                        person.getUser().getProfile().getFullName(), Integer.toString(numberOfMissionDays),
                        Integer.toString(numberOfPersonelExpenseDays)));
            } else {
                // Cross-check accomodations and personel expenses
                final int numberOfFullPersonelExpenseDays =
                        FullPersonelExpenseItem.calculateNumberOfFullPersonelExpenseDays(this, person);
                final int numberOfAccomodatedNights = AccommodationItem.calculateNumberOfAccomodatedNights(this, person);
                if (numberOfMissionDays < numberOfFullPersonelExpenseDays + numberOfAccomodatedNights) {
                    result.add(BundleUtil.getString("resources/MissionResources",
                            "message.mission.full.personel.expense.days.not.match", person.getUser().getProfile().getFullName()));
                }
            }
        }

        // Check all financers have an accounting unit
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            if (missionFinancer.getUnit() == null || missionFinancer.getAccountingUnit() == null) {
                final String unitName = missionFinancer.getUnit() != null ? missionFinancer.getUnit().getPresentationName() : "";
                result.add(BundleUtil.getString("resources/MissionResources", "message.mission.financer.with.no.accounting.unit",
                        unitName));
            }
        }

        return result;
    }

    private boolean hasValidDates(final OtherPersonelExpenseItem missionItem) {
        final DateTime start = missionItem.getStart();
        final DateTime end = missionItem.getEnd();
        return start != null && end != null && start.isBefore(end);
    }

    private boolean hasAnyPersonelExpenseItems(final Person person) {
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (missionItem instanceof PersonelExpenseItem && !(missionItem instanceof NoPersonelExpenseItem)) {
                if (missionItem.getPeopleSet().contains(person)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int calculateNumberOfNights() {
        final LocalDate startDate = getDaparture().toLocalDate();
        final LocalDate endDate = getArrival().toLocalDate();
        return Days.daysBetween(startDate, endDate).getDays();
    }

    public boolean allParticipantsAreAuthorized() {
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (!personMissionAuthorization.isAuthorized()) {
                return false;
            }
        }
        return true;
    }

    public boolean isOnTime() {
        return getDaparture().minusDays(15).isAfterNow();
    }

    public Integer getNunberOfLunchesToDiscount(final Person person) {
        return getNunberOfLunchesToDiscount();
/*	int result = 0;
	if (hasAnyMissionItems()) {
for (final MissionItem missionItem : getMissionItemsSet()) {
		if (missionItem.isPersonelExpenseItem()) {
		    final PersonelExpenseItem personelExpenseItem = (PersonelExpenseItem) missionItem;
		    result += personelExpenseItem.getNunberOfLunchesToDiscount(person);
		}
}
	} else {
result = getNunberOfLunchesToDiscount();
	}
	return Integer.valueOf(result);
 */
    }

    private int getNunberOfLunchesToDiscount() {
        int result = 0;
        for (DateTime dateTime = getDaparture(); !dateTime.toDateMidnight().isAfter(getArrival().toDateMidnight()); dateTime =
                dateTime.plusDays(1)) {
            if (discountLunchDay(dateTime)) {
                result++;
            }
        }
        return result;
    }

    protected boolean discountLunchDay(final DateTime dateTime) {
        final int dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek != DateTimeConstants.SATURDAY && dayOfWeek != DateTimeConstants.SUNDAY && !isHoliday(dateTime);
    }

    private boolean isHoliday(final DateTime dateTime) {
        // TODO Possibly refactor this and place data in the repository...
        //      also this does not yet account for mobile holidays and local
        //      holidays depending on the persons working place.
        final int year = dateTime.getYear();
        final int monthOfYear = dateTime.getMonthOfYear();
        final int dayOfMonth = dateTime.getDayOfMonth();
        return (monthOfYear == 1 && dayOfMonth == 1) || (monthOfYear == 4 && dayOfMonth == 25)
                || (monthOfYear == 5 && dayOfMonth == 1) || (monthOfYear == 6 && dayOfMonth == 10)
                || (monthOfYear == 8 && dayOfMonth == 15) || (monthOfYear == 10 && dayOfMonth == 5)
                || (monthOfYear == 11 && dayOfMonth == 1) || (monthOfYear == 12 && dayOfMonth == 1)
                || (monthOfYear == 12 && dayOfMonth == 8) || (monthOfYear == 12 && dayOfMonth == 25)
                || (year == 2011 && monthOfYear == 4 && dayOfMonth == 22)
                || (year == 2011 && monthOfYear == 6 && dayOfMonth == 13)
                || (year == 2011 && monthOfYear == 6 && dayOfMonth == 23);
    }

    public int getNunberOfLunchesToDiscountOnFirstPersonelExpenseDay(final PersonelExpenseItem personelExpenseItem) {
        return 1;
    }

    public int getNunberOfLunchesToDiscountOnLastPersonelExpenseDay(final PersonelExpenseItem personelExpenseItem) {
        return 1;
    }

    public DailyPersonelExpenseTable getDailyPersonelExpenseTable() {
        return DailyPersonelExpenseTable.findDailyPersonelExpenseTableFor(getClass(), new LocalDate());
    }

    public boolean getWithSalary(final Person participant) {
        return !getParticipantesWithoutSalarySet().contains(participant);
    }

    public boolean areAllParticipantsAuthorized() {
        if (getPersonMissionAuthorizationsSet().size() == 0) {
            return false;
        }
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (!personMissionAuthorization.isAuthorized()) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllParticipantsAuthorizedForPhaseOne() {
        if (getPersonMissionAuthorizationsSet().size() == 0) {
            return false;
        }
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (!personMissionAuthorization.isPreAuthorized()) {
                return false;
            }
        }
        return true;
    }

    public int getPersonAuthorizationChainSize(final Person person) {
        final PersonMissionAuthorization personMissionAuthorization = getPersonMissionAuthorization(person);
        return personMissionAuthorization == null ? 0 : personMissionAuthorization.getChainSize();
    }

    public boolean isExpenditureAuthorized() {
        if (!hasAnyFinancer()) {
            return false;
        }
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            if (!missionFinancer.isAuthorized()) {
                return false;
            }
        }
        return true;
    }

    public String getMissionResponsibleName() {
        return hasMissionResponsible() ? getMissionResponsible().getPartyName().getContent() : null;
    }

    public Money getTotalCost() {
        Money result = Money.ZERO;
        for (final MissionItem missionItem : getMissionItemsSet()) {
            result = result.add(missionItem.getValue());
        }
        return result;
    }

    public Money getTotalPrevisionaryCost() {
        Money result = Money.ZERO;
        for (final MissionItem missionItem : getMissionItemsSet()) {
            result = result.add(missionItem.getPrevisionaryCosts());
        }
        return result;
    }

    public int getDurationInDays() {
        return Days.daysBetween(getDaparture(), getArrival()).getDays();
    }

    public boolean isPendingParticipantAuthorisationBy(final User user) {
        final Person person = user.getPerson();
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (isPendingParticipantAuthorisationBy(person, personMissionAuthorization)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPendingParticipantAuthorisationBy(Person person, PersonMissionAuthorization personMissionAuthorization) {
        final AccountabilityType workingAccountabilityType = personMissionAuthorization.getWorkingAccountabilityType();
        final LocalDate now = new LocalDate();
        for (PersonMissionAuthorization p = personMissionAuthorization; p != null; p = p.getNext()) {
            if (p.isAvailableForAuthorization() && p.getAuthority() == null && p.getDelegatedAuthority() == null) {
                final module.organization.domain.Unit unit = p.getUnit();
                return unit.getChildAccountabilityStream().anyMatch(a -> a.isActive(now) && a.getChild() == person
                        && isResponsibleAccountabilityType(a.getAccountabilityType(), workingAccountabilityType));
            }
        }
        return false;
    }

    private boolean isResponsibleAccountabilityType(final AccountabilityType accountabilityType,
            final AccountabilityType workingAccountabilityType) {
        final MissionSystem missionSystem = MissionSystem.getInstance();
        for (final MissionAuthorizationAccountabilityType missionAuthorizationAccountabilityType : missionSystem
                .getMissionAuthorizationAccountabilityTypesSet()) {
            if ((missionAuthorizationAccountabilityType.getAccountabilityType() == workingAccountabilityType
                    || workingAccountabilityType == null)
                    && missionAuthorizationAccountabilityType.getAccountabilityTypesSet().contains(accountabilityType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthorized() {
        if (!hasAnyMissionItems()) {
            return false;
        }
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            if (!missionFinancer.isAuthorized()) {
                return false;
            }
        }
        return true;
    }

    public String getDestinationDescription() {
        return getLocation();
    }

    public boolean isAccountingEmployee(final pt.ist.expenditureTrackingSystem.domain.organization.Person expenditurePerson) {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            final AccountingUnit accountingUnit = missionFinancer.getAccountingUnit();
            if (accountingUnit != null && accountingUnit.getPeopleSet().contains(expenditurePerson)) {
                return true;
            }
        }
        return false;
    }

    public boolean isProjectAccountingEmployee(
            final pt.ist.expenditureTrackingSystem.domain.organization.Person expenditurePerson) {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            if (missionFinancer.isProjectFinancer()) {
                final AccountingUnit accountingUnit = missionFinancer.getAccountingUnit();
                if (accountingUnit.getProjectAccountantsSet().contains(expenditurePerson)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDirectProjectAccountingEmployee(
            final pt.ist.expenditureTrackingSystem.domain.organization.Person expenditurePerson) {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            final Person person = expenditurePerson.getUser() != null ? expenditurePerson.getUser().getPerson() : null;
            if (missionFinancer.isProjectFinancer() && missionFinancer.isAccountManager(person)) {
                final AccountingUnit accountingUnit = missionFinancer.getAccountingUnit();
                if (accountingUnit.getProjectAccountantsSet().contains(expenditurePerson)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isParticipantResponsible(final Person person) {
        if (person != null) {
            final LocalDate now = new LocalDate();
            for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
                if (isResponsibleFor(now, person, personMissionAuthorization)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isResponsibleFor(final LocalDate now, final Person person,
            final PersonMissionAuthorization personMissionAuthorization) {
        final module.organization.domain.Unit unit = personMissionAuthorization.getUnit();
        if (person.getParentAccountabilityStream().anyMatch(a -> a.isActive(now) && a.getParent() == unit
                && getMissionSystem().isAccountabilityTypesThatAuthorize(a.getAccountabilityType()))) {
            return true;
        }
        return personMissionAuthorization.getNext() != null ? isResponsibleFor(now, person,
                personMissionAuthorization.getNext()) : false;
    }

    public boolean isFinancerAccountant(final pt.ist.expenditureTrackingSystem.domain.organization.Person person) {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            final AccountingUnit accountingUnit = missionFinancer.getAccountingUnit();
            if (accountingUnit != null) {
                if (accountingUnit.getPeopleSet().contains(person) || accountingUnit.getProjectAccountantsSet().contains(person)
                        || accountingUnit.getTreasuryMembersSet().contains(person)
                        || accountingUnit.getResponsiblePeopleSet().contains(person)
                        || accountingUnit.getResponsibleProjectAccountantsSet().contains(person)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isFinancerResponsible(final pt.ist.expenditureTrackingSystem.domain.organization.Person expenditurePerson) {
        for (final MissionFinancer missionFinancer : getFinancerSet()) {
            final Unit unit = missionFinancer.getUnit();
            if (unit != null && unit.isResponsible(expenditurePerson)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPersonelSectionMember(final User user) {
        for (final AccountabilityTypeQueue accountabilityTypeQueue : MissionSystem.getInstance()
                .getAccountabilityTypeQueuesSet()) {
            final WorkflowQueue workflowQueue = accountabilityTypeQueue.getWorkflowQueue();
            if (workflowQueue.isUserAbleToAccessQueue(user)) {
                return true;
            }
        }
        return false;
    }

    public SortedSet<MissionChangeDescription> getSortedMissionChangeDescriptions() {
        final SortedSet<MissionChangeDescription> result =
                new TreeSet<MissionChangeDescription>(MissionChangeDescription.COMPARATOR_BY_WHEN);
        result.addAll(getMissionChangeDescriptionSet());
        return result;
    }

    public Set<String> getAllFundAllocations() {
        final Set<String> result = new TreeSet<String>();
        final String fundAllocationLog = getFundAllocationLog();
        if (fundAllocationLog != null && !fundAllocationLog.isEmpty()) {
            final String[] fundAllocations = fundAllocationLog.split(";");
            for (final String fundAllocation : fundAllocations) {
                result.add(fundAllocation);
            }
        }
        return result;
    }

    public void registerFundAllocation(final String newFundAllocation) {
        final Set<String> fundAllocations = getAllFundAllocations();
        fundAllocations.add(newFundAllocation);
        registerFundAllocation(fundAllocations);
    }

    protected void registerFundAllocation(final Set<String> fundAllocations) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String fundAllocation : fundAllocations) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(';');
            }
            stringBuilder.append(fundAllocation);
        }
        setFundAllocationLog(stringBuilder.toString());
    }

    public MissionVersion getMissionVersion() {
        MissionVersion result = null;
        MissionVersion last = null;
        for (final MissionVersion missionVersion : getMissionVersionsSet()) {
            final DateTime since = missionVersion.getSinceDateTime();
            if (!since.isAfterNow()) {
                if (result == null || result.getSinceDateTime().isBefore(since)) {
                    result = missionVersion;
                }
            }
            if (last == null || last.getSinceDateTime().isAfter(since)) {
                last = missionVersion;
            }
        }
        return result == null ? last : result;
    }

    public void migrate() {
        if (getMissionVersionsSet().size() == 0) {
            final MissionVersion missionVersion = new MissionVersion(this);
            final DateTime firstOperation = findFirstOperations();
            missionVersion.setSinceDateTime(firstOperation);
            missionVersion.setDates(super.getDaparture(), super.getArrival());

            //missionVersion.getMissionItemsSet().addAll(super.getMissionItemsSet());
            //missionVersion.getFinancerSet().addAll(super.getFinancerSet());
        }

        for (final MissionVersion missionVersion : getMissionVersionsSet()) {
            if (missionVersion.getIsArchived() == null) {
                missionVersion.setIsArchived(Boolean.FALSE);
            }
        }
    }

    private DateTime findFirstOperations() {
        return getMissionProcess().getExecutionLogStream().min(WorkflowLog.COMPARATOR_BY_WHEN)
                .map(l -> l.getWhenOperationWasRan()).orElseGet(() -> new DateTime());
    }

    public int getFinancerCount() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getFinancerSet().size();
    }

    public boolean hasAnyFinancer() {
        final MissionVersion missionVersion = getMissionVersion();
        return !missionVersion.getFinancerSet().isEmpty();
    }

    public Set<MissionFinancer> getFinancerSet() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getFinancerSet();
    }

    public int getMissionItemsCount() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getMissionItemsSet().size();
    }

    public boolean hasAnyMissionItems() {
        final MissionVersion missionVersion = getMissionVersion();
        return !missionVersion.getMissionItemsSet().isEmpty();
    }

    public boolean hasAnyVehicleItems() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.hasAnyVehicleItems();
    }

    public Set<MissionItem> getMissionItemsSet() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getMissionItemsSet();
    }

    public boolean isReadyForMissionTermination() {
        return getArrival().isBeforeNow() && !sentForTermination() && areAllParticipantsAuthorized()
                && (!hasAnyFinancer() || areAllFundsAuthorized());
    }

    private boolean areAllFundsAuthorized() {
        return isAuthorized();
    }

    private boolean sentForTermination() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getChangesAfterArrival() != null;
    }

    public void sendForProcessTermination(final String descriptionOfChangesAfterArrival) {
        final MissionVersion missionVersion;
        if (descriptionOfChangesAfterArrival == null || descriptionOfChangesAfterArrival.isEmpty()) {
            missionVersion = getMissionVersion();
            missionVersion.setChangesAfterArrival(Boolean.FALSE);
        } else {
            missionVersion = new MissionVersion(this);
            missionVersion.setChangesAfterArrival(Boolean.TRUE);
            missionVersion.setDescriptionOfChangesAfterArrival(descriptionOfChangesAfterArrival);
        }

        missionVersion.autoArchive();
    }

    public boolean isTerminatedWithChanges() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.isTerminatedWithChanges();
    }

    public boolean isArchived() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.getIsArchived().booleanValue();
    }

    public boolean isTerminated() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.isTerminated();
    }

    public boolean canArchiveMission() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.canArchiveMission();
    }

    public boolean canArchiveMissionDirect() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.canArchiveMissionDirect();
    }

    public boolean isRequestorOrResponsible() {
        final User user = Authenticate.getUser();
        final Person person = user == null ? null : user.getPerson();
        return person == getRequestingPerson() || person == getMissionResponsible();
    }

    public boolean hasNoItemsAndParticipantesAreAuthorized() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion.hasNoItemsAndParticipantesAreAuthorized();
    }

    public boolean isReadyToHaveAssociatedPaymentProcesses() {
        return isAuthorized() && hasAnyNonPersonalExpenseMissionItems() && areAllParticipantsAuthorized();
    }

    private boolean hasAnyNonPersonalExpenseMissionItems() {
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (!missionItem.isPersonelExpenseItem()) {
                return true;
            }
        }
        return false;
    }

    public void revertProcessTermination() {
        final MissionVersion missionVersion = getMissionVersion();
        final String descriptionOfChangesAfterArrival = missionVersion.getDescriptionOfChangesAfterArrival();
        if (descriptionOfChangesAfterArrival == null || descriptionOfChangesAfterArrival.isEmpty()) {
            missionVersion.setChangesAfterArrival(null);
        } else {
            final MissionVersion newMissionVersion = new MissionVersion(this);
            newMissionVersion.setChangesAfterArrival(null);
            newMissionVersion.setDescriptionOfChangesAfterArrival(null);
        }

        missionVersion.unArchive();
    }

    public boolean isUnitObserver(final User user) {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.isUnitObserver(user)) {
                return true;
            }
        }
        for (final PersonMissionAuthorization personMissionAuthorization : getPersonMissionAuthorizationsSet()) {
            if (personMissionAuthorization.isUnitObserver(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAccountManager(final Person accountManager) {
        if (accountManager != null) {
            final User user = accountManager.getUser();
            if (user != null) {
                final pt.ist.expenditureTrackingSystem.domain.organization.Person person = user.getExpenditurePerson();
                if (person != null) {
                    for (final MissionFinancer financer : getFinancerSet()) {
                        if (financer.getAccountManager() == accountManager.getUser().getExpenditurePerson()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean getAreAccomodationItemsAvailable() {
        return !getGrantOwnerEquivalence().booleanValue() && this instanceof ForeignMission;
    }

    public boolean getPersonelExpenseItemsAvailable() {
        return !getGrantOwnerEquivalence().booleanValue();
    }

    public boolean canTogleMissionNature() {
        return (getGrantOwnerEquivalence().booleanValue())
                || (MissionSystem.getInstance().allowGrantOwnerEquivalence() && !hasAnyPersonalExpenseOrAccomodationItemns());
    }

    private boolean hasAnyPersonalExpenseOrAccomodationItemns() {
        for (final MissionItem missionItem : getMissionItemsSet()) {
            if (missionItem instanceof AccommodationItem) {
                return true;
            }
            if (missionItem instanceof PersonelExpenseItem && !(missionItem instanceof NoPersonelExpenseItem)) {
                return true;
            }
        }
        return false;
    }

    public int getNumberOfDays() {
        int result = 1;
        for (DateTime d = getDaparture(); d.isBefore(getArrival()); d = d.plusDays(1)) {
            result++;
        }
        return result;
    }

    public void checkForAnyOverlappingParticipations() {
        for (final Person person : getParticipantesSet()) {
            for (final Mission mission : person.getMissionsSet()) {
                final MissionProcess process = mission.getMissionProcess();
                if (mission != this && !process.isCanceled() && overlaps(mission)) {
                    throw new DomainException(Bundle.EXPENDITURE,
                            BundleUtil.getString("resources/MissionResources", "error.mission.overlaps.participation",
                                    person.getPresentationName(), process.getProcessIdentification(),
                                    ExpenditureTrackingSystem.getInstance().getInstitutionManagementEmail()));
                }
            }
        }
    }

    public Interval getInterval() {
        final DateTime departure = getDaparture();
        final DateTime arrival = getArrival();
        return new Interval(departure, arrival);
    }

    public boolean overlaps(final Mission mission) {
        final Interval interval = mission.getInterval();
        return getInterval().overlaps(interval);
    }

    public boolean hasAnyCurrentRelationToInstitution(final Person person) {
        final LocalDate arrivalDate = getArrival().toLocalDate();
        return person.getParentAccountabilityStream()
                .anyMatch(a -> a.isActive(arrivalDate) && a.isValid() && MissionSystem.REQUIRE_AUTHORIZATION_PREDICATE.test(a));
    }

    public String getCurrentRelationToInstitution(final Person person) {
        final LocalDate arrivalDate = getArrival().toLocalDate();
        final StringBuilder builder = new StringBuilder();
        person.getParentAccountabilityStream()
                .filter(a -> a.isActive(arrivalDate) && a.isValid() && MissionSystem.REQUIRE_AUTHORIZATION_PREDICATE.test(a))
                .forEach(new Consumer<Accountability>() {
                    @Override
                    public void accept(Accountability a) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(a.getAccountabilityType().getName().getContent());
                    }
                });
        return builder.length() == 0 ? BundleUtil.getString("resources/MissionResources",
                "label.participant.no.relation.to.institution") : builder.toString();
    }

    public boolean hasCommitmentNumber() {
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.getCommitmentNumber() != null && !financer.getCommitmentNumber().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllCommitmentNumbers() {
        final Boolean requireCommitmentNumber = ExpenditureTrackingSystem.getInstance().getRequireCommitmentNumber();
        if (requireCommitmentNumber != null && !requireCommitmentNumber.booleanValue()) {
            return true;
        }
        for (final MissionFinancer financer : getFinancerSet()) {
            if (financer.getCommitmentNumber() == null || financer.getCommitmentNumber().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public String getAccountingUnitsAsString() {
        final StringBuilder builder = new StringBuilder();
        for (final MissionFinancer financer : getFinancerSet()) {
            final AccountingUnit accountingUnit = financer.getAccountingUnit();
            if (accountingUnit != null) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(accountingUnit.getName());
            }
        }
        return builder.toString();
    }

    public Set<MissionItem> getMissionItems() {
        final MissionVersion missionVersion = getMissionVersion();
        return missionVersion == null ? Collections.<MissionItem> emptySet() : missionVersion.getMissionItemsSet();
    }

    @Deprecated
    public java.util.Set<module.mission.domain.MissionVersion> getMissionVersions() {
        return getMissionVersionsSet();
    }

    @Deprecated
    public java.util.Set<module.mission.domain.PersonMissionAuthorization> getPersonMissionAuthorizations() {
        return getPersonMissionAuthorizationsSet();
    }

    @Deprecated
    public java.util.Set<module.organization.domain.Person> getParticipantes() {
        return getParticipantesSet();
    }

    @Deprecated
    public java.util.Set<module.mission.domain.MissionChangeDescription> getMissionChangeDescription() {
        return getMissionChangeDescriptionSet();
    }

    @Deprecated
    public java.util.Set<module.organization.domain.Person> getParticipantesWithoutSalary() {
        return getParticipantesWithoutSalarySet();
    }

    @Deprecated
    public boolean hasAnyMissionVersions() {
        return !getMissionVersionsSet().isEmpty();
    }

    @Deprecated
    public boolean hasAnyPersonMissionAuthorizations() {
        return !getPersonMissionAuthorizationsSet().isEmpty();
    }

    @Deprecated
    public boolean hasAnyParticipantes() {
        return !getParticipantesSet().isEmpty();
    }

    @Deprecated
    public boolean hasAnyMissionChangeDescription() {
        return !getMissionChangeDescriptionSet().isEmpty();
    }

    @Deprecated
    public boolean hasAnyParticipantesWithoutSalary() {
        return !getParticipantesWithoutSalarySet().isEmpty();
    }

    @Deprecated
    public boolean hasLocation() {
        return getLocation() != null;
    }

    @Deprecated
    public boolean hasObjective() {
        return getObjective() != null;
    }

    @Deprecated
    public boolean hasGrantOwnerEquivalence() {
        return getGrantOwnerEquivalence() != null;
    }

    @Deprecated
    public boolean hasIsApprovedByMissionResponsible() {
        return getIsApprovedByMissionResponsible() != null;
    }

    @Deprecated
    public boolean hasFundAllocationLog() {
        return getFundAllocationLog() != null;
    }

    @Deprecated
    public boolean hasServiceGaranteeInstante() {
        return getServiceGaranteeInstante() != null;
    }

    @Deprecated
    public boolean hasDaparture() {
        return getDaparture() != null;
    }

    @Deprecated
    public boolean hasArrival() {
        return getArrival() != null;
    }

    @Deprecated
    public boolean hasRequestingUnit() {
        return getRequestingUnit() != null;
    }

    @Deprecated
    public boolean hasMissionResponsible() {
        return getMissionResponsible() != null;
    }

    @Deprecated
    public boolean hasRequestingPerson() {
        return getRequestingPerson() != null;
    }

    @Deprecated
    public boolean hasMissionSystem() {
        return getMissionSystem() != null;
    }

    @Deprecated
    public boolean hasApprovalForMissionWithNoFinancers() {
        return getApprovalForMissionWithNoFinancers() != null;
    }

    @Deprecated
    public boolean hasServiceGaranteePerson() {
        return getServiceGaranteePerson() != null;
    }

    @Deprecated
    public boolean hasMissionProcess() {
        return getMissionProcess() != null;
    }

    public boolean participantsBelongToInstitution() {
        for (final Person person : getParticipantesSet()) {
            if (hasAnyCurrentRelationToInstitution(person)) {
                return true;
            }
        }
        return false;
    }

    public Money getMaxDailyAccomodationValue() {
        return null;
    }

}
