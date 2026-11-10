package org.openmrs.module.digipath.connector.proforma;

import net.openclinical.beans.Code;
import net.openclinical.beans.Coding;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DigipathUtils {
	
	public static Concept getConceptByCode(Code code) {
		ConceptService conceptService = Context.getService(ConceptService.class);
		Concept concept = null;
		
		for (Coding coding : code.getCoding()) {
			
			if (coding.getSystem() != null) {
				switch (coding.getSystem()) {
					case "https://cielterminology.org":
					case "https://cielterminology.org/":
						concept = getConcept("CIEL", coding.getCode());
						break;
					default:
						concept = conceptService.getConceptByUuid(coding.getCode());
						break;
				}
			} else if (coding.getCode() != null) {
				concept = conceptService.getConceptByUuid(coding.getCode());
			}
			
			if (concept != null)
				return concept;
		}
		
		return concept;
		
	}
	
	public static Set<Concept> getConceptListByCode(Code code) {
		ConceptService conceptService = Context.getService(ConceptService.class);
		Set<Concept> conceptSet =new HashSet<>();

		if (code == null || code.getCoding() == null) {
			return conceptSet;
		}

		for (Coding coding : code.getCoding()) {
			if (coding == null || coding.getCode() == null) {
				continue;
			}

			String system = coding.getSystem();
			String codingCode = coding.getCode();
			Concept concept = null;

			if ("https://cielterminology.org".equals(system)
					|| "https://cielterminology.org/".equals(system)) {
				concept = getConcept("CIEL", codingCode);
			}

			if (concept == null) {
				concept = conceptService.getConceptByUuid(codingCode);
			}

			if (concept != null) {
				conceptSet.add(concept);
			}
		}
		return conceptSet;

	}
	
	public static Concept getConcept(String system, String code) {
		
		ConceptService conceptService = Context.getService(ConceptService.class);
		ConceptSource source = Context.getConceptService().getConceptSourceByName(system);
		
		ConceptReferenceTerm term = conceptService.getConceptReferenceTermByCode(code, source);
		
		if (term == null) {
			return null;
		}
		
		return conceptService.getConceptByMapping(term.getCode(), source.getName(), false);
		
	}
}
