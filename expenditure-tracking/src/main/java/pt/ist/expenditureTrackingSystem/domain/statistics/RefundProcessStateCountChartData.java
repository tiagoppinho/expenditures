/*
 * @(#)RefundProcessStateCountChartData.java
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
package pt.ist.expenditureTrackingSystem.domain.statistics;

import java.math.BigDecimal;

import pt.ist.expenditureTrackingSystem.domain.acquisitions.PaymentProcess;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.PaymentProcessYear;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.RefundProcessStateType;
import pt.ist.expenditureTrackingSystem.domain.acquisitions.refund.RefundProcess;
import pt.ist.expenditureTrackingSystem.util.Calculation.Operation;

/**
 * 
 * @author Luis Cruz
 * 
 */
public class RefundProcessStateCountChartData extends RefundProcessStateTypeChartData {

    public RefundProcessStateCountChartData(final PaymentProcessYear paymentProcessYear) {
        super(paymentProcessYear, Operation.SUM);
    }

    @Override
    protected String getTitleKey() {
        return "label.number.processes";
    }

    @Override
    protected void count(final PaymentProcess paymentProcess) {
        if (paymentProcess.isRefundProcess()) {
            final RefundProcess refundProcess = (RefundProcess) paymentProcess;
            final RefundProcessStateType refundProcessStateType = refundProcess.getProcessState().getRefundProcessStateType();
            calculation.registerValue(refundProcessStateType, new BigDecimal(1));
        }
    }

}
