package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

import java.util.List;

public interface ConceptProvider {

    public Concept getConcept(final OntResource o);

    public List<? extends OntResource> getConcepts(final OntModel o);

    public List<? extends OntResource> getMoreGeneralConcepts(final OntResource resource);
}
