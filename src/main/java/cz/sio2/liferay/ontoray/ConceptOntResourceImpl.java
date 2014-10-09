package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class ConceptOntResourceImpl extends Concept {

    private OntResource ontResource;

    public ConceptOntResourceImpl(final OntResource ontResource) {
        this.ontResource = ontResource;
    }

    @Override
    public Map<Locale, String> getLabel() {
        return getLangToStringMap(ontResource, RDFS.label.toString());
    }

    @Override
    public Map<Locale, String> getDescription() {
        return getLangToStringMap(ontResource, RDFS.comment.toString());
    }

    @Override
    public Map<String, String> getProperties() {
        final StmtIterator i = ontResource.listProperties();
        final Map<String,String> map = new HashMap<>();

        map.put("iri",ontResource.getURI());
//
//        while (i.hasNext()) {
//            final Statement statement = i.next();
//
//            if ( RDFS.label.equals(statement.getPredicate())) {
//                continue;
//            }
//
//            if ( RDFS.comment.equals(statement.getPredicate())) {
//                continue;
//            }
//
//            map.put(statement.getPredicate().getURI(),statement.getObject().toString());
//        }

        return map;
    }
}
