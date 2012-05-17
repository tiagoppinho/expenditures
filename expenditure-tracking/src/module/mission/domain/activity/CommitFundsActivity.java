package module.mission.domain.activity;

import module.mission.domain.MissionFinancer;
import module.mission.domain.MissionProcess;
import module.mission.domain.activity.CommitFundsActivityInformation.MissionFinancerCommitFundAllocationBean;
import module.workflow.activities.ActivityInformation;
import myorg.domain.User;
import myorg.util.BundleUtil;

public class CommitFundsActivity extends MissionProcessActivity<MissionProcess, CommitFundsActivityInformation> {

    @Override
    public String getLocalizedName() {
	return BundleUtil.getStringFromResourceBundle("resources/MissionResources", "activity." + getClass().getSimpleName());
    }

    @Override
    public boolean isActive(final MissionProcess missionProcess, final User user) {
	return super.isActive(missionProcess, user)
		&& !missionProcess.getIsCanceled().booleanValue()
		&& missionProcess.getMission().hasAnyFinancer()
		&& missionProcess.hasAllAllocatedFunds()
		&& !missionProcess.hasAllCommitmentNumbers()
		&& missionProcess.isAccountingEmployee(user.getExpenditurePerson());
    }

    @Override
    protected void process(final CommitFundsActivityInformation commitFundsActivityInformation) {
	for (final MissionFinancerCommitFundAllocationBean missionFinancerCommitFundAllocationBean : commitFundsActivityInformation.getMissionFinancerCommitFundAllocationBeans()) {
	    final MissionFinancer missionFinancer = missionFinancerCommitFundAllocationBean.getMissionFinancer();
	    final String commitmentNumber = missionFinancerCommitFundAllocationBean.getCommitmentNumber();
	    missionFinancer.setCommitmentNumber(commitmentNumber);
	}
    }

    @Override
    public ActivityInformation<MissionProcess> getActivityInformation(final MissionProcess process) {
	return new CommitFundsActivityInformation(process, this);
    }

    @Override
    public boolean isDefaultInputInterfaceUsed() {
	return false;
    }

}
