<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html"%>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean"%>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/workflow" prefix="wf"%>

<%@ page import="java.math.BigDecimal" %>

<bean:define id="workingCapital" name="process" property="workingCapital"/>

<h3>
	<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="label.module.workingCapital.initialization"/>
</h3>

<bean:define id="workingCapitalInitialization" name="workingCapital" property="workingCapitalInitialization" type="module.workingCapital.domain.WorkingCapitalInitialization"/>
<bean:define id="workingCapitalOid" name="workingCapital" property="externalId"/>
<bean:define id="workingCapitalInitializationOid" type="java.lang.String" name="workingCapitalInitialization" property="externalId"/>

<div class="infobox col2-1">
	<fr:view name="workingCapital" property="workingCapitalInitialization">
		<fr:schema bundle="WORKING_CAPITAL_RESOURCES"  type="module.workingCapital.domain.WorkingCapitalInitialization">
			<fr:slot name="workingCapital.accountingUnit.name" key="label.module.workingCapital.initialization.accountingUnit"/>
			<fr:slot name="requestCreation" key="label.module.workingCapital.requestingDate"/>
			<fr:slot name="requestor.name"  key="label.module.workingCapital.requester"/>
			<logic:present name="workingCapital" property="movementResponsible">
				<fr:slot name="workingCapital.movementResponsible.name" key="label.module.workingCapital.movementResponsible"/>
			</logic:present>
			<fr:slot name="fiscalId"  key="label.module.workingCapital.fiscalId"/>
			<fr:slot name="internationalBankAccountNumber"  key="label.module.workingCapital.internationalBankAccountNumber"/>
			<fr:slot name="fundAllocationId" key="label.module.workingCapital.fundAllocationId"/>
			<fr:slot name="requestedAnualValue" key="label.module.workingCapital.requestedAnualValue.requested"/>
			<fr:slot name="requestedAnualValue.roundedValue" layout="format" key="label.module.workingCapital.requestedMonthlyValue.requested">
				<fr:property name="format" value="<%= workingCapitalInitialization.getRequestedAnualValue().divideAndRound(new BigDecimal(6)).toFormatString() %>"/>
			</fr:slot>
			<fr:slot name="authorizedAnualValue"  key="label.module.workingCapital.authorizedAnualValue" layout="null-as-label">
				<fr:property name="subLayout" value=""/>
				<fr:property name="classes" value="bold"/>
			</fr:slot>
			<fr:slot name="maxAuthorizedAnualValue"  key="label.module.workingCapital.maxAuthorizedAnualValue" layout="null-as-label">
				<fr:property name="subLayout" value=""/>
			</fr:slot>
			<logic:present name="workingCapital" property="workingCapitalInitialization.lastSubmission">
				<fr:slot name="lastSubmission" key="label.module.workingCapital.initialization.lastSubmission"/>
			</logic:present>
			<logic:present name="workingCapital" property="workingCapitalInitialization.refundRequested">
				<fr:slot name="refundRequested" key="label.module.workingCapital.initialization.refundRequested"/>
			</logic:present>
			<logic:equal name="workingCapital" property="workingCapitalInitialization.class.name" value="module.workingCapital.domain.WorkingCapitalInitializationReenforcement">
				<fr:slot name="requestedReenforcementValue" key="label.module.workingCapital.initialization.requestedReenforcementValue"/>
				<fr:slot name="authorizedReenforcementValue" key="label.module.workingCapital.initialization.authorizedReenforcementValue" layout="null-as-label">
					<fr:property name="subLayout" value=""/>
					<fr:property name="classes" value="bold"/>
				</fr:slot>
			</logic:equal>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="process-info mbottom0"/>
			<fr:property name="columnClasses" value="aleft width215px,,"/>
		</fr:layout>
	</fr:view>
	<wf:activityLink processName="process" activityName="EditInitializationActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
		<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.EditInitializationActivity"/>
	</wf:activityLink>
</div>

<div class="infobox2 col2-2">
	<fr:view name="process" layout="process-state-view"/>
</div>

<bean:size id="capitalInitializationsCount" name="workingCapital"  property="workingCapitalInitializations"/>
<logic:greaterThan name="capitalInitializationsCount" value="1">
	<html:link page='<%= "/workingCapital.do?method=viewAllCapitalInitializations&workingCapitalOid=" + workingCapitalOid %>'><bean:message key="label.module.workingCapital.showAllWorkingCapitalInitializationForProcess" bundle="WORKING_CAPITAL_RESOURCES"/></html:link>
</logic:greaterThan>
		
			<table class="tstyle3 mtop1 mbottom1 width100pc" >
				<tr><th><bean:message key="label.module.workingCapital.operations" bundle="WORKING_CAPITAL_RESOURCES"/></th>
				<th><bean:message key="label.module.workingCapital.date" bundle="WORKING_CAPITAL_RESOURCES"/></th>
				<th><bean:message key="label.module.workingCapital.person" bundle="WORKING_CAPITAL_RESOURCES"/></th>
				<th></th></tr>
				<tr>
					<td class="aleft">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="label.module.workingCapital.initialization.aprovalByUnitResponsible"/>
					</td>
					<td>
						<logic:present name="workingCapitalInitialization" property="aprovalByUnitResponsible">
							<fr:view name="workingCapitalInitialization" property="aprovalByUnitResponsible"/>
						</logic:present>
						<logic:notPresent name="workingCapitalInitialization" property="aprovalByUnitResponsible">
							-
						</logic:notPresent>
					</td>
					<td>
					<logic:present name="workingCapitalInitialization" property="responsibleForUnitApproval">
						<fr:view name="workingCapitalInitialization" property="responsibleForUnitApproval.person.firstAndLastName"/>
					</logic:present>
					<logic:notPresent name="workingCapitalInitialization" property="responsibleForUnitApproval">
						-
					</logic:notPresent>
					</td>
					<td>
					<logic:equal name="process" property="workingCapital.workingCapitalInitialization.pendingAproval" value="true">
						<wf:activityLink processName="process" activityName="ApproveActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
							<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.ApproveActivity"/>
						</wf:activityLink>
					</logic:equal>
					<wf:activityLink processName="process" activityName="UnApproveActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.UnApproveActivity"/>
					</wf:activityLink>
					</td>
				</tr>
				
				
				<tr>
					<td class="aleft">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="label.module.workingCapital.initialization.verificationByAccounting"/>
					</td>
					<td>
						<logic:present name="workingCapitalInitialization" property="verificationByAccounting">
							<fr:view name="workingCapitalInitialization" property="verificationByAccounting"/>
						</logic:present>
						<logic:notPresent name="workingCapitalInitialization" property="verificationByAccounting">
							-
						</logic:notPresent>
					</td>
					<td>
					<logic:present name="workingCapitalInitialization" property="responsibleForAccountingVerification">
						<fr:view name="workingCapitalInitialization" property="responsibleForAccountingVerification.firstAndLastName"/>
					</logic:present>
					<logic:notPresent name="workingCapitalInitialization" property="responsibleForAccountingVerification">
						-
					</logic:notPresent>
					</td>
					<td>
					<wf:activityLink processName="process" activityName="VerifyActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.VerifyActivity"/>
					</wf:activityLink>
					<wf:activityLink processName="process" activityName="UnVerifyActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.UnVerifyActivity"/>
					</wf:activityLink>
					</td>
				</tr>
				
				<tr>
					<td class="aleft">
						<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="label.module.workingCapital.initialization.authorizationByUnitResponsible"/>
					</td>
					<td>
						<logic:present name="workingCapitalInitialization" property="authorizationByUnitResponsible">
							<fr:view name="workingCapitalInitialization" property="authorizationByUnitResponsible"/>
						</logic:present>
						<logic:notPresent name="workingCapitalInitialization" property="authorizationByUnitResponsible">
							-
						</logic:notPresent>
					</td>
					<td>
					<logic:present name="workingCapitalInitialization" property="responsibleForUnitAuthorization">
						<fr:view name="workingCapitalInitialization" property="responsibleForUnitAuthorization.child.firstAndLastName"/>
					</logic:present>
					<logic:notPresent name="workingCapitalInitialization" property="responsibleForUnitAuthorization">
						-
					</logic:notPresent>
					</td>
					<td>
						<wf:activityLink processName="process" activityName="AuthorizeActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
							<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.AuthorizeActivity"/>
						</wf:activityLink>
						<wf:activityLink processName="process" activityName="UnAuthorizeActivity" scope="request" paramName0="workingCapitalInitialization" paramValue0="<%= workingCapitalInitializationOid %>">
							<bean:message bundle="WORKING_CAPITAL_RESOURCES" key="activity.UnAuthorizeActivity"/>
						</wf:activityLink>
					</td>
				</tr>
			</table>