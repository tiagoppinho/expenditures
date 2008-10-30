update GENERIC_LOG SET OJB_CONCRETE_CLASS = 'pt.ist.expenditureTrackingSystem.domain.acquisitions.OperationLog' where OJB_CONCRETE_CLASS = 'pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.SimplifiedProcessOperationLog' OR OJB_CONCRETE_CLASS = 'pt.ist.expenditureTrackingSystem.domain.acquisitions.standard.StandardProcessOperationLog';

update PROCESS_STATE SET OJB_CONCRETE_CLASS = 'pt.ist.expenditureTrackingSystem.domain.acquisitions.AcquisitionProcessState' WHERE OJB_CONCRETE_CLASS='pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.SimplifiedAcquitionProcessState' OR OJB_CONCRETE_CLASS='pt.ist.expenditureTrackingSystem.domain.acquisitions.standard.StandardAcquitionProcessState'
