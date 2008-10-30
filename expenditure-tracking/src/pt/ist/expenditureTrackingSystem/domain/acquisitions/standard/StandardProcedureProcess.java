package pt.ist.expenditureTrackingSystem.domain.acquisitions.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ist.expenditureTrackingSystem.domain.DomainException;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.AcquisitionProcessStateType;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.AcquisitionRequest;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.StandardAcquisitionProcessStateType;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.activities.GenericAcquisitionProcessActivity;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.AddPayingUnit;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.ApproveAcquisitionProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.AssignPayingUnitToItem;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.CreateAcquisitionRequestItem;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.DeleteAcquisitionProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.DeleteAcquisitionRequestItem;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.EditAcquisitionRequestItem;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.RejectAcquisitionProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.RemovePayingUnit;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities.SubmitForApproval;
import pt.ist.expenditureTrackingSystem.domain.dto.CreateAcquisitionProcessBean;
import pt.ist.expenditureTrackingSystem.domain.organization.Person;
import pt.ist.expenditureTrackingSystem.domain.organization.Supplier;
import pt.ist.expenditureTrackingSystem.domain.organization.Unit;
import pt.ist.expenditureTrackingSystem.domain.processes.AbstractActivity;
import pt.ist.expenditureTrackingSystem.domain.processes.GenericProcess;
import pt.ist.expenditureTrackingSystem.domain.util.Money;
import pt.ist.fenixWebFramework.services.Service;

public class StandardProcedureProcess extends StandardProcedureProcess_Base {

    private static Money PROCESS_VALUE_LIMIT = new Money("75000");

    public StandardProcedureProcess() {
	super();
    }

    private static Map<ActivityScope, List<GenericAcquisitionProcessActivity>> activities = new HashMap<ActivityScope, List<GenericAcquisitionProcessActivity>>();

    static {
	List<GenericAcquisitionProcessActivity> requestInformationActivities = new ArrayList<GenericAcquisitionProcessActivity>();
	List<GenericAcquisitionProcessActivity> requestItemActivities = new ArrayList<GenericAcquisitionProcessActivity>();

	// requestInformationActivities.add(new
	// CreateAcquisitionPurchaseOrderDocument());
	// requestInformationActivities.add(new SendPurchaseOrderToSupplier());
	// requestInformationActivities.add(new SkipPurchaseOrderDocument());

	// requestInformationActivities.add(new
	// AddAcquisitionProposalDocument());
	requestInformationActivities.add(new CreateAcquisitionRequestItem());
	requestInformationActivities.add(new AddPayingUnit());
	requestInformationActivities.add(new RemovePayingUnit());
	requestInformationActivities.add(new DeleteAcquisitionProcess());
	requestInformationActivities.add(new SubmitForApproval());

	// requestInformationActivities.add(new SubmitForFundAllocation());
	// requestInformationActivities.add(new FundAllocationExpirationDate());

	requestInformationActivities.add(new ApproveAcquisitionProcess());
	requestInformationActivities.add(new RejectAcquisitionProcess());

	// requestInformationActivities.add(new
	// AllocateProjectFundsPermanently());
	// requestInformationActivities.add(new AllocateFundsPermanently());
	// requestInformationActivities.add(new
	// RemoveFundsPermanentlyAllocated());
	// requestInformationActivities.add(new UnApproveAcquisitionProcess());

	// requestInformationActivities.add(new ProjectFundAllocation());
	// requestInformationActivities.add(new FundAllocation());
	// requestInformationActivities.add(new RemoveFundAllocation());
	// requestInformationActivities.add(new RemoveProjectFundAllocation());
	// requestInformationActivities.add(new
	// RemoveFundAllocationExpirationDate());
	// requestInformationActivities.add(new CancelAcquisitionRequest());

	// requestInformationActivities.add(new PayAcquisition());
	// requestInformationActivities.add(new ReceiveInvoice());
	// requestInformationActivities.add(new FixInvoice());
	// requestInformationActivities.add(new SubmitForConfirmInvoice());
	// requestInformationActivities.add(new ConfirmInvoice());
	// requestInformationActivities.add(new UnSubmitForApproval());

	requestItemActivities.add(new DeleteAcquisitionRequestItem());
	requestItemActivities.add(new EditAcquisitionRequestItem());
	requestItemActivities.add(new AssignPayingUnitToItem());
	// requestItemActivities.add(new
	// EditAcquisitionRequestItemRealValues());
	// requestItemActivities.add(new DistributeRealValuesForPayingUnits());

	// requestItemActivities.add(new ChangeFinancersAccountingUnit());

	activities.put(ActivityScope.REQUEST_INFORMATION, requestInformationActivities);
	activities.put(ActivityScope.REQUEST_ITEM, requestItemActivities);

    }

    protected StandardProcedureProcess(final Person requester) {
	super();
	inGenesis();
	new AcquisitionRequest(this, requester);
    }

    protected StandardProcedureProcess(List<Supplier> suppliers, Person person) {
	super();
	inGenesis();
	new AcquisitionRequest(this, suppliers, person);
    }

    @Override
    public GenericAcquisitionProcessActivity getActivityByName(String activityName) {

	for (ActivityScope scope : activities.keySet()) {
	    for (GenericAcquisitionProcessActivity activity : activities.get(scope)) {
		if (activity.getName().equals(activityName)) {
		    return activity;
		}
	    }
	}
	return null;
    }

    @Service
    public static StandardProcedureProcess createNewAcquisitionProcess(
	    final CreateAcquisitionProcessBean createAcquisitionProcessBean) {
	if (!isCreateNewProcessAvailable()) {
	    throw new DomainException("acquisitionProcess.message.exception.invalidStateToRun.create");
	}
	StandardProcedureProcess process = new StandardProcedureProcess(createAcquisitionProcessBean.getSuppliers(),
		createAcquisitionProcessBean.getRequester());
	process.getAcquisitionRequest().setRequestingUnit(createAcquisitionProcessBean.getRequestingUnit());
	if (createAcquisitionProcessBean.isRequestUnitPayingUnit()) {
	    final Unit unit = createAcquisitionProcessBean.getRequestingUnit();
	    process.getAcquisitionRequest().addFinancers(unit.finance(process.getAcquisitionRequest()));
	}

	return process;
    }

    public List<GenericAcquisitionProcessActivity> getActiveActivities(ActivityScope scope) {
	List<GenericAcquisitionProcessActivity> activitiesResult = new ArrayList<GenericAcquisitionProcessActivity>();
	for (GenericAcquisitionProcessActivity activity : activities.get(scope)) {
	    if (activity.isActive(this)) {
		activitiesResult.add(activity);
	    }
	}
	return activitiesResult;
    }

    public List<GenericAcquisitionProcessActivity> getActiveActivitiesForItem() {
	return getActiveActivities(ActivityScope.REQUEST_ITEM);
    }

    public List<GenericAcquisitionProcessActivity> getActiveActivitiesForRequest() {
	return getActiveActivities(ActivityScope.REQUEST_INFORMATION);
    }

    @Override
    public Map<ActivityScope, List<GenericAcquisitionProcessActivity>> getProcessActivityMap() {
	return activities;
    }

    @Override
    public Money getAcquisitionRequestValueLimit() {
	return PROCESS_VALUE_LIMIT;
    }

    @Override
    public List<AcquisitionProcessStateType> getAvailableStates() {
	List<AcquisitionProcessStateType> availableStates = new ArrayList<AcquisitionProcessStateType>();
	availableStates.add(AcquisitionProcessStateType.IN_GENESIS);
	availableStates.add(AcquisitionProcessStateType.SUBMITTED_FOR_APPROVAL);
	availableStates.add(AcquisitionProcessStateType.SUBMITTED_FOR_FUNDS_ALLOCATION);
	availableStates.add(AcquisitionProcessStateType.FUNDS_ALLOCATED_TO_SERVICE_PROVIDER);
	availableStates.add(AcquisitionProcessStateType.FUNDS_ALLOCATED);
	availableStates.add(AcquisitionProcessStateType.APPROVED);
	availableStates.add(AcquisitionProcessStateType.INVITES_SENT);
	availableStates.add(AcquisitionProcessStateType.IN_NEGOTIATION);
	availableStates.add(AcquisitionProcessStateType.NEGOTIATION_ENDED);
	availableStates.add(AcquisitionProcessStateType.SELECTED_SERVICE_PROVIDER);
	availableStates.add(AcquisitionProcessStateType.DOCUMENTATION_INSERTED);
	availableStates.add(AcquisitionProcessStateType.ACQUISITION_PROCESSED);
	availableStates.add(AcquisitionProcessStateType.INVOICE_RECEIVED);
	availableStates.add(AcquisitionProcessStateType.SUBMITTED_FOR_CONFIRM_INVOICE);
	availableStates.add(AcquisitionProcessStateType.INVOICE_CONFIRMED);
	availableStates.add(AcquisitionProcessStateType.FUNDS_ALLOCATED_PERMANENTLY);
	availableStates.add(AcquisitionProcessStateType.ACQUISITION_PAYED);
	availableStates.add(AcquisitionProcessStateType.REJECTED);
	availableStates.add(AcquisitionProcessStateType.CANCELED);
	return availableStates;
    }
}
