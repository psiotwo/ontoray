package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.Map1;

import java.util.List;

/**
 * Created by kremep1 on 10/8/14.
 */
public class ConceptProviderSKOSTerminology implements ConceptProvider {
    @Override
    public Concept getConcept(OntResource o) {
        return new ConceptSKOSImpl(o);
    }

    @Override
    public List<? extends OntResource> getConcepts(OntModel o) {
        return o.listIndividuals(ResourceFactory.createResource(ConceptSKOSImpl.SKOS_CONCEPT)).toList();
    }

    @Override
    public List<? extends OntResource> getMoreGeneralConcepts(OntResource resource) {
        return resource.listPropertyValues(ResourceFactory.createProperty(ConceptSKOSImpl.SKOS_BROADER)).mapWith(new Map1<RDFNode, OntResource>() {
            @Override
            public OntResource map1(RDFNode o) {
                return o.as(OntResource.class);
            }
        }).toList();
    }
}
