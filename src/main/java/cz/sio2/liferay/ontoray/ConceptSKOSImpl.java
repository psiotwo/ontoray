package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class ConceptSKOSImpl extends Concept {

    public static String SKOS_CORE="http://www.w3.org/2004/02/skos/core#";
    public static String SKOS_PREF_LABEL=SKOS_CORE+ "prefLabel";
    public static String SKOS_DEFINITION=SKOS_CORE+ "definition";
    public static String SKOS_CONCEPT=SKOS_CORE+ "Concept";
    public static String SKOS_BROADER=SKOS_CORE+ "broader";

    private OntResource ontResource;

    public ConceptSKOSImpl(final OntResource ontResource) {
        this.ontResource = ontResource;
    }

    @Override
    public Map<Locale, String> getLabel() {
        return getLangToStringMap(ontResource,SKOS_PREF_LABEL);
    }

    @Override
    public Map<Locale, String> getDescription() {
        return getLangToStringMap(ontResource, SKOS_DEFINITION);
    }

    @Override
    public Map<String, String> getProperties() {
        final StmtIterator i = ontResource.listProperties();
        final Map<String,String> map = new HashMap<>();

        map.put("iri",ontResource.getURI());

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
