package mr.supnum.bib.impl;

import jakarta.jws.WebService;
import mr.supnum.bib.contrat.EmpruntPortType;
import mr.supnum.bib.contrat.EmpruntType;
import mr.supnum.bib.contrat.LivreIndisponible;
import mr.supnum.bib.contrat.LivreIndisponibleFault;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

@com.sun.xml.ws.developer.SchemaValidation
@WebService(
    serviceName = "EmpruntService",
    portName = "EmpruntPort",
    targetNamespace = "http://supnum.mr/bib",
    wsdlLocation = "/wsdl/EmpruntService.wsdl",
    endpointInterface = "mr.supnum.bib.contrat.EmpruntPortType"
)
public class EmpruntServiceImpl implements EmpruntPortType {

    @Override
    public EmpruntType emprunterLivre(String idAdherent, String isbn) throws LivreIndisponibleFault {
        if (!"978-2-10-074444-4".equals(isbn)) {
            LivreIndisponible faultInfo = new LivreIndisponible();
            faultInfo.setIsbn(isbn);
            faultInfo.setRetourPrevu(date(LocalDate.now().plusDays(7)));
            throw new LivreIndisponibleFault("Livre indisponible : " + isbn, faultInfo);
        }
        EmpruntType e = new EmpruntType();
        e.setNumero("E-2026-0812");
        e.setDateRetour(date(LocalDate.now().plusDays(14)));
        return e;
    }

    private static XMLGregorianCalendar date(LocalDate d) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(d.toString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}