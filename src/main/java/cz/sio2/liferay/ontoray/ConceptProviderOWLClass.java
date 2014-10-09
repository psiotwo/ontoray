package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

import java.util.List;

public class ConceptProviderOWLClass implements ConceptProvider {

    public Concept getConcept(final OntResource o) {
        return new ConceptOntResourceImpl(o);
    }

    public List<? extends OntResource> getConcepts(final OntModel o) {
        return o.listClasses().toList();
    }

    public List<? extends OntResource> getMoreGeneralConcepts(final OntResource resource) {
        return resource.asClass().listSuperClasses(true).toList();
    }
}
