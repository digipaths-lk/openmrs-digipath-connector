package org.openmrs.module.digipath.connector.proforma;

import net.openclinical.beans.Code;
import net.openclinical.beans.DataDefinition;
import net.openclinical.beans.Fhir;
import net.openclinical.beans.Range;
import net.openclinical.proforma.enactment.EnactmentOptions;
import org.openmrs.Concept;
import org.openmrs.Condition;
import org.openmrs.Patient;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class ConditionEvaluator implements DataDefinitionEvaluator {
	
	ConditionService conditionService = Context.getService(ConditionService.class);
	
	@Override
	public List<EnactmentOptions.TimestampedValue> evaluate(Fhir fhir, Patient patient, String value, boolean isMultiValue,
	        List<Range> rangeList) {
		
		System.out.println(" ConditionEvaluator " + 111111);
		List<EnactmentOptions.TimestampedValue> list;
		;
		switch (fhir.getElement()) {
			case "code":
				list = extractAllDataForCode(fhir.getCode(), patient, value);
				break;
			default:
				throw new IllegalArgumentException();
		}
		return list;
	}
	
	private List<EnactmentOptions.TimestampedValue> extractAllDataForCode(Code code, Patient patient, String value) {
		Concept concept = DigipathUtils.getConceptByCode(code);
		List<EnactmentOptions.TimestampedValue> list = new ArrayList<>();
		List<Condition> conditionList = conditionService.getActiveConditions(patient);

		Stream<Condition> filteredList =  conditionList.stream().filter(condition -> condition.getCondition().getCoded() == concept);

		Optional<Condition> optionalCondition = filteredList.findFirst();
        Date date = optionalCondition.map(Condition::getOnsetDate).orElse(null);

		if(date == null)
			date = optionalCondition.map(Condition::getDateCreated).orElse(null);

		if(date != null)
			 list.add(new EnactmentOptions.TimestampedValue(date.toInstant(), Collections.singletonList(value)));

		if(!list.isEmpty())
			return list;
		return null;

    }
}
