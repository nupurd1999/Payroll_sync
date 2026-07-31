package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.PayrollBatch;
import com.nupoor.payrollsync.entity.PayrollItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SepaXmlGeneratorService {

    private final String initiatingPartyName;
    private final String debtorName;
    private final String debtorIban;
    private final String debtorBic;

    public SepaXmlGeneratorService(
            @Value("${payroll.sepa.initiating-party-name:Enterprise PayrollSync Corp}") String initiatingPartyName,
            @Value("${payroll.sepa.debtor-name:Enterprise PayrollSync Corp HQ}") String debtorName,
            @Value("${payroll.sepa.debtor-iban:DE89370400440532013000}") String debtorIban,
            @Value("${payroll.sepa.debtor-bic:COBA234XXX}") String debtorBic) {
        this.initiatingPartyName = initiatingPartyName;
        this.debtorName = debtorName;
        this.debtorIban = debtorIban;
        this.debtorBic = debtorBic;
    }

    public String generatePain001Xml(PayrollBatch batch, List<PayrollItem> items) {
        String msgId = "MSG-" + batch.getBatchReference() + "-" + System.currentTimeMillis();
        String creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        int numberOfTransactions = items.size();
        BigDecimal controlSum = batch.getTotalNet();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n");
        xml.append("  <CstmrCdtTrfInitn>\n");

        // Group Header
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(escapeXml(msgId)).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(creationDateTime).append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>").append(numberOfTransactions).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(controlSum.toPlainString()).append("</CtrlSum>\n");
        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>").append(escapeXml(initiatingPartyName)).append("</Nm>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");

        // Payment Information
        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>PMT-").append(escapeXml(batch.getBatchReference())).append("</PmtInfId>\n");
        xml.append("      <PmtMtd>TRF</PmtMtd>\n");
        xml.append("      <NbOfTxs>").append(numberOfTransactions).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(controlSum.toPlainString()).append("</CtrlSum>\n");
        xml.append("      <PmtTpInf>\n");
        xml.append("        <SvcLvl><Cd>SEPA</Cd></SvcLvl>\n");
        xml.append("      </PmtTpInf>\n");
        xml.append("      <ReqdExctnDt>").append(LocalDateTime.now().toLocalDate().toString()).append("</ReqdExctnDt>\n");
        xml.append("      <Dbtr>\n");
        xml.append("        <Nm>").append(escapeXml(debtorName)).append("</Nm>\n");
        xml.append("      </Dbtr>\n");
        xml.append("      <DbtrAcct>\n");
        xml.append("        <Id><IBAN>").append(escapeXml(debtorIban)).append("</IBAN></Id>\n");
        xml.append("      </DbtrAcct>\n");
        xml.append("      <DbtrAgt>\n");
        xml.append("        <FinInstnId><BIC>").append(escapeXml(debtorBic)).append("</BIC></FinInstnId>\n");
        xml.append("      </DbtrAgt>\n");
        xml.append("      <ChrgBr>SLEV</ChrgBr>\n");

        // Credit Transfer Transaction Information (Items)
        for (PayrollItem item : items) {
            xml.append("      <CdtTrfTxInf>\n");
            xml.append("        <PmtId>\n");
            xml.append("          <EndToEndId>E2E-").append(item.getId().toString().substring(0, 8)).append("</EndToEndId>\n");
            xml.append("        </PmtId>\n");
            xml.append("        <Amt>\n");
            xml.append("          <InstdAmt Ccy=\"EUR\">").append(item.getNetSalary().toPlainString()).append("</InstdAmt>\n");
            xml.append("        </Amt>\n");
            xml.append("        <CdtrAgt>\n");
            xml.append("          <FinInstnId><BIC>").append(escapeXml(item.getEmployee().getBic())).append("</BIC></FinInstnId>\n");
            xml.append("        </CdtrAgt>\n");
            xml.append("        <Cdtr>\n");
            xml.append("          <Nm>").append(escapeXml(item.getEmployee().getFirstName() + " " + item.getEmployee().getLastName())).append("</Nm>\n");
            xml.append("        </Cdtr>\n");
            xml.append("        <CdtrAcct>\n");
            xml.append("          <Id><IBAN>").append(escapeXml(item.getEmployee().getIban())).append("</IBAN></Id>\n");
            xml.append("        </CdtrAcct>\n");
            xml.append("        <RmtInf>\n");
            xml.append("          <Ustrd>Salary Payout Period ").append(batch.getPayrollPeriod()).append(" Ref ").append(item.getEmployee().getEmployeeCode()).append("</Ustrd>\n");
            xml.append("        </RmtInf>\n");
            xml.append("      </CdtTrfTxInf>\n");
        }

        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrCdtTrfInitn>\n");
        xml.append("</Document>");

        return xml.toString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
