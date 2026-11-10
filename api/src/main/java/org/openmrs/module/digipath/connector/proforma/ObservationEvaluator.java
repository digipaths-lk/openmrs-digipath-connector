package org.openmrs.module.digipath.connector.proforma;

import net.openclinical.beans.Code;
import net.openclinical.beans.DataDefinition;
import net.openclinical.beans.Fhir;
import net.openclinical.beans.Range;
import net.openclinical.proforma.Protocol;
import net.openclinical.proforma.enactment.EnactmentOptions;
import net.openclinical.proforma.tasks.Task;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ObservationEvaluator implements DataDefinitionEvaluator {
	
	ObsService obsService = Context.getService(ObsService.class);
	
	@Override
	public List<EnactmentOptions.TimestampedValue> evaluate(Fhir fhir, Patient patient, String value, boolean isMultiValue,
	        List<Range> rangeList) {
		
		List<EnactmentOptions.TimestampedValue> list;
		switch (fhir.getElement()) {
			case "code":
				list = extractDataByPatientAndCode(fhir.getCode(), patient, true, rangeList);
				break;
			default:
				throw new IllegalArgumentException();
		}
		
		return list;
	}
	
	private List<EnactmentOptions.TimestampedValue> extractDataByPatientAndCode(Code code, Patient patient, boolean isMultiValue, List<Range> rangeList) {
		Concept concept = DigipathUtils.getConceptByCode(code);

		List<Obs> obsList = obsService.getObservationsByPersonAndConcept(patient, concept);
		List<EnactmentOptions.TimestampedValue> timestampedValueList = new ArrayList<>();

		if(obsList.isEmpty())
			return timestampedValueList;

		obsList.forEach(obs -> {
			if(obs.getValueNumeric() != null && (isMultiValue || timestampedValueList.isEmpty())) {
				timestampedValueList.add(new EnactmentOptions.TimestampedValue(obs.getDateCreated().toInstant(), obs.getValueNumeric()));
			}else if(obs.getValueCoded() != null && rangeList != null && (isMultiValue || timestampedValueList.isEmpty())) {
				rangeList.forEach(range -> {
					if(range.getMeta() != null && range.getMeta().getFhir() != null) {
						Concept rangeConcept = DigipathUtils.getConceptByCode(range.getMeta().getFhir().getCode());
						if(rangeConcept.getUuid().equals(obs.getValueCoded().getUuid())){
							timestampedValueList.add(new EnactmentOptions.TimestampedValue(obs.getDateCreated().toInstant(), range.getValue()));
							}
						}
				});
			}
		});
		return timestampedValueList.stream()
				.sorted(Comparator.comparing(EnactmentOptions.TimestampedValue::getWhen))
				.collect(Collectors.toList());

    }
}
