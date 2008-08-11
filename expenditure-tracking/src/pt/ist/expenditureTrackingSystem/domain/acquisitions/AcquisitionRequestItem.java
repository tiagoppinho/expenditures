package pt.ist.expenditureTrackingSystem.domain.acquisitions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import pt.ist.expenditureTrackingSystem.domain.ExpenditureTrackingSystem;
import pt.ist.expenditureTrackingSystem.domain.dto.AcquisitionRequestItemBean;
import pt.ist.expenditureTrackingSystem.domain.organization.DeliveryInfo;
import pt.ist.expenditureTrackingSystem.domain.organization.Person;
import pt.ist.expenditureTrackingSystem.domain.organization.Unit;
import pt.ist.expenditureTrackingSystem.domain.util.Address;
import pt.ist.fenixWebFramework.services.Service;
import pt.ist.fenixframework.pstm.Transaction;

public class AcquisitionRequestItem extends AcquisitionRequestItem_Base {

    public AcquisitionRequestItem(final AcquisitionRequest acquisitionRequest) {
	super();
	setExpenditureTrackingSystem(ExpenditureTrackingSystem.getInstance());
	setAcquisitionRequest(acquisitionRequest);
    }

    private AcquisitionRequestItem(final AcquisitionRequest acquisitionRequest, final String description, final Integer quantity,
	    final BigDecimal unitValue, final BigDecimal vatValue, final String proposalReference, String salesCode) {
	this(acquisitionRequest);
	setDescription(description);
	setQuantity(quantity);
	setUnitValue(unitValue);
	setVatValue(vatValue);
	setProposalReference(proposalReference);
	setSalesCode(salesCode);
    }

    public AcquisitionRequestItem(final AcquisitionRequestItemBean acquisitionRequestItemBean) {
	this(acquisitionRequestItemBean.getAcquisitionRequest(), acquisitionRequestItemBean.getDescription(),
		acquisitionRequestItemBean.getQuantity(), acquisitionRequestItemBean.getUnitValue(), acquisitionRequestItemBean
			.getVatValue(), acquisitionRequestItemBean.getProposalReference(), acquisitionRequestItemBean
			.getSalesCode());
	setDeliveryInfo(acquisitionRequestItemBean);
    }

    protected void setDeliveryInfo(AcquisitionRequestItemBean acquisitionRequestItemBean) {
	String recipient;
	Address address;
	if (acquisitionRequestItemBean.getDeliveryInfo() != null) {
	    recipient = acquisitionRequestItemBean.getDeliveryInfo().getRecipient();
	    address = acquisitionRequestItemBean.getDeliveryInfo().getAddress();
	} else {
	    recipient = acquisitionRequestItemBean.getRecipient();
	    address = acquisitionRequestItemBean.getAddress();
	    acquisitionRequestItemBean.getAcquisitionRequest().getRequester().createNewDeliveryInfo(recipient, address);
	}
	setRecipient(recipient);
	setAddress(address);

    }

    public AcquisitionRequestItem(final AcquisitionRequest acquisitionRequest, final String description, final Integer quantity,
	    final BigDecimal unitValue, final BigDecimal vatValue, final String proposalReference, String salesCode,
	    String recipient, Address address) {
	this(acquisitionRequest, description, quantity, unitValue, vatValue, proposalReference, salesCode);
	setRecipient(recipient);
	setAddress(address);
    }

    public BigDecimal getTotalItemValue() {
	final BigDecimal unitValue = getUnitValue();
	final Integer quantity = getQuantity();
	return multiply(unitValue, quantity);
    }

    public BigDecimal getTotalItemValueWithVat() {
	BigDecimal totalValue = getTotalItemValue();
	BigDecimal vatValue = totalValue.multiply(getVatValue().divide(new BigDecimal(100)));
	return totalValue.add(vatValue);
    }

    public BigDecimal getTotalAssignedValue() {
	BigDecimal sum = BigDecimal.ZERO;
	for (UnitItem unitItem : getUnitItems()) {
	    sum = sum.add(unitItem.getShareValue());
	}
	return sum;
    }

    private BigDecimal multiply(final BigDecimal unitValue, final Integer quantity) {
	return unitValue == null || quantity == null ? BigDecimal.ZERO : unitValue.multiply(new BigDecimal(quantity.intValue()));
    }

    public void edit(String description, Integer quantity, BigDecimal unitValue, BigDecimal vatValue, String proposalReference,
	    String salesCode, DeliveryInfo deliveryInfo) {
	setDescription(description);
	setQuantity(quantity);
	setUnitValue(unitValue);
	setProposalReference(proposalReference);
	setSalesCode(salesCode);
	setVatValue(vatValue);
	setRecipient(deliveryInfo.getRecipient());
	setAddress(deliveryInfo.getAddress());
    }

    public void edit(AcquisitionRequestItemBean acquisitionRequestItemBean) {
	setDescription(acquisitionRequestItemBean.getDescription());
	setQuantity(acquisitionRequestItemBean.getQuantity());
	setUnitValue(acquisitionRequestItemBean.getUnitValue());
	setProposalReference(acquisitionRequestItemBean.getProposalReference());
	setSalesCode(acquisitionRequestItemBean.getSalesCode());
	setVatValue(acquisitionRequestItemBean.getVatValue());
	setDeliveryInfo(acquisitionRequestItemBean);

    }

    @Service
    public void delete() {
	removeAcquisitionRequest();
	removeExpenditureTrackingSystem();
	for (; !getUnitItems().isEmpty(); getUnitItems().get(0).delete())
	    ;
	Transaction.deleteObject(this);
    }

    public boolean isAssignedTo(Unit unit) {
	for (UnitItem unitItem : getUnitItems()) {
	    if (unitItem.getUnit() == unit) {
		return true;
	    }
	}
	return false;
    }

    public UnitItem getUnitItemFor(Unit unit) {
	for (UnitItem unitItem : getUnitItems()) {
	    if (unitItem.getUnit() == unit) {
		return unitItem;
	    }
	}
	return null;
    }

    public boolean isValueFullyAttributedToUnits() {
	BigDecimal totalValue = BigDecimal.ZERO;
	for (UnitItem unitItem : getUnitItems()) {
	    totalValue = totalValue.add(unitItem.getShareValue());
	}

	return totalValue.compareTo(getTotalItemValue()) == 0;
    }

    public void createUnitItem(Unit unit, BigDecimal shareValue) {
	new UnitItem(unit, this, shareValue, Boolean.FALSE);
    }

    public List<Unit> getPayingUnits() {
	List<Unit> payingUnits = new ArrayList<Unit>();
	for (UnitItem unitItem : getUnitItems()) {
	    payingUnits.add(unitItem.getUnit());
	}
	return payingUnits;
    }

    private void modifyApprovingStateFor(Person person, Boolean value) {
	for (UnitItem unitItem : getUnitItems()) {
	    if (unitItem.getUnit().isResponsible(person)) {
		unitItem.setItemApproved(value);
	    }
	}
    }

    public void approvedBy(Person person) {
	modifyApprovingStateFor(person, Boolean.TRUE);
    }

    public void unapprovedBy(Person person) {
	modifyApprovingStateFor(person, Boolean.FALSE);
    }

    public boolean isApproved() {
	for (UnitItem unitItem : getUnitItems()) {
	    if (!unitItem.getItemApproved()) {
		return false;
	    }
	}
	return true;
    }

    public boolean hasBeenApprovedBy(Person person) {
	for (UnitItem unitItem : getUnitItems()) {
	    if (unitItem.getUnit().isResponsible(person) && unitItem.getItemApproved()) {
		return true;
	    }
	}
	return false;
    }

}
