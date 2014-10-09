package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class Concept {

    public abstract Map<Locale,String> getLabel();

    public abstract Map<Locale,String> getDescription();

    public abstract  Map<String,String> getProperties();

    protected Map<Locale,String> getLangToStringMap(final OntResource entity, final String property) {
        final Map<Locale,String> titleMap = new HashMap<>();
        final OntProperty pLabel = entity.getOntModel().getOntProperty(property);

        for (final Statement label : entity.listProperties(pLabel).toList() ) {
            if ( label.getObject().canAs(Literal.class)) {
                Literal owlLiteral = label.getObject().asLiteral();
                titleMap.put(owlLiteral.getLanguage() != null ? Locale.forLanguageTag(owlLiteral.getLanguage()) : null, owlLiteral.getLexicalForm());
            }
        }
        return titleMap;
    }
}
