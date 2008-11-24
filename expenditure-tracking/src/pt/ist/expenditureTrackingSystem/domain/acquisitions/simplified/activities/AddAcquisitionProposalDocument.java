package pt.ist.expenditureTrackingSystem.domain.acquisitions.simplified.activities;

import pt.ist.expenditureTrackingSystem.applicationTier.Authenticate.User;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.AcquisitionRequest;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.RegularAcquisitionProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.activities.GenericAcquisitionProcessActivity;
import pt.ist.fenixWebFramework.security.UserView;

public class AddAcquisitionProposalDocument extends GenericAcquisitionProcessActivity {

    @Override
    protected boolean isAccessible(RegularAcquisitionProcess process) {
	User user = UserView.getUser();
	return user != null && user.getPerson().equals(process.getRequestor());
    }

    @Override
    protected boolean isAvailable(RegularAcquisitionProcess process) {
	final AcquisitionRequest acquisitionRequest = process.getAcquisitionRequest();
	return super.isAvailable(process) && process.getAcquisitionProcessState().isInGenesis()
		&& acquisitionRequest.getAcquisitionProposalDocument() == null;
    }

    @Override
    protected void process(RegularAcquisitionProcess process, Object... objects) {
	final AcquisitionRequest acquisitionRequest = process.getAcquisitionRequest();
	String filename = (String) objects[0];
	byte[] bytes = (byte[]) objects[1];
	String proposalID = (String) objects[2];
	acquisitionRequest.addAcquisitionProposalDocument(filename, bytes, proposalID);
    }

}
