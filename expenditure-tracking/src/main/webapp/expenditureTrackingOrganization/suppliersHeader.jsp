<%@page import="pt.ist.expenditureTrackingSystem.domain.ExpenditureTrackingSystem"%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<div class="infobox_dotted">
    <ul>
        <% if (ExpenditureTrackingSystem.isManager() || ExpenditureTrackingSystem.isAcquisitionCentralManagerGroupMember()
                    || ExpenditureTrackingSystem.isAcquisitionCentralGroupMember()
                    || ExpenditureTrackingSystem.isAcquisitionsProcessAuditorGroupMember()
                    || ExpenditureTrackingSystem.isSupplierManagerGroupMember()) { %>
            <% if (ExpenditureTrackingSystem.isManager()) { %>
				<li>
					<html:link action="/expenditureTrackingOrganization.do?method=prepareCreateSupplier">
						<bean:message key="supplier.link.create" bundle="EXPENDITURE_ORGANIZATION_RESOURCES"/>
					</html:link>		
				</li>
			<% } %>
			<li>
				<html:link action="/expenditureTrackingOrganization.do?method=listSuppliers">
					<bean:message key="supplier.link.list" bundle="EXPENDITURE_ORGANIZATION_RESOURCES"/>
				</html:link>
			</li>
        <% } %>
        <li>
            <html:link action="/expenditureTrackingOrganization.do?method=listCPVReferences">
                <bean:message key="supplier.link.list.cpv" bundle="EXPENDITURE_ORGANIZATION_RESOURCES"/>
            </html:link>
        </li>
        <% if (!ExpenditureTrackingSystem.getInstance().getMaterialsSet().isEmpty()) { %>
			<li>
				<html:link action="/expenditureTrackingOrganization.do?method=listMaterials">
					<bean:message key="supplier.link.list.material" bundle="EXPENDITURE_ORGANIZATION_RESOURCES"/>
				</html:link>
			</li>
        <% } %>
    </ul>
</div>
