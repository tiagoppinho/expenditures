package pt.ist.expenditureTrackingSystem.domain.acquisitions.consultation.activities;

import org.fenixedu.bennu.core.domain.User;

import module.workflow.activities.ActivityInformation;
import module.workflow.activities.WorkflowActivity;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.consultation.MultipleSupplierConsultationProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.consultation.MultipleSupplierConsultationProcessState;

public class SelectSupplier extends WorkflowActivity<MultipleSupplierConsultationProcess, SelectSupplierInformation> {

    @Override
    public boolean isActive(final MultipleSupplierConsultationProcess process, final User user) {
        return process.getState() == MultipleSupplierConsultationProcessState.PENDING_SUPPLIER_SELECTION;
    }

    @Override
    protected void process(final SelectSupplierInformation information) {
        final MultipleSupplierConsultationProcess process = information.getProcess();
        process.setState(MultipleSupplierConsultationProcessState.SUPPLIERS_SELECTED);
    }

    @Override
    public ActivityInformation<MultipleSupplierConsultationProcess> getActivityInformation( final MultipleSupplierConsultationProcess process) {
        return new SelectSupplierInformation(process, this);
    }

    @Override
    public String getUsedBundle() {
        return "resources/ExpenditureResources";
    }

}