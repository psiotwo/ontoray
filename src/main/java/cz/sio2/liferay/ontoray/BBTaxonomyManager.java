package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import org.primefaces.model.UploadedFile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.util.*;

@ManagedBean(name = "bbtaxonomymanager")
@ViewScoped
public class BBTaxonomyManager {

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void upload() {
        if (file != null) {
            final OntModel mdl = ModelFactory.createOntologyModel();

            try {
                mdl.read(file.getInputstream(), null);
                transformToCategories(mdl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void transformToCategories(OntModel o) {
        try {
            final ServiceContext serviceContext = LiferayFacesContext.getInstance().getServiceContext();
            final String testonto = escapeForAssetString("GEN " + DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()));

            final User user = PortalUtil.getUser(LiferayFacesContext.getInstance().getPortletRequest());

            Map<Locale, String> titleMap = new HashMap<Locale, String>();
            ensureNotEmpty(titleMap,testonto);
            Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
            String settings = "";

            final Map<String,AssetCategory> map = new HashMap<>();

            final AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.addVocabulary( user.getUserId(),testonto, titleMap, descriptionMap,settings,serviceContext);

            for(OntClass c : o.listClasses().toList()) {
                getCategoryForClass(c, map, user.getUserId(), vocabulary.getVocabularyId(), serviceContext);
            }
        } catch (PortalException e) {
            e.printStackTrace();
        } catch (SystemException e) {
            e.printStackTrace();
        }
    }

    private Map<Locale,String> getLangToStringMap(final OntResource entity, final String property) {
        final Map<Locale,String> titleMap = new HashMap<Locale, String>();
        final OntProperty pLabel = entity.getOntModel().getOntProperty(property);

        for (final Statement label : entity.listProperties(pLabel).toList() ) {
            Literal owlLiteral = label.getObject().asLiteral();
            titleMap.put(owlLiteral.getLanguage() != null ? Locale.forLanguageTag(owlLiteral.getLanguage()) : null, owlLiteral.getLexicalForm());
        }
        return titleMap;
    }

    private void ensureNotEmpty( final Map<Locale, String> map, final String defString) {
        if (map.keySet().size() > 0 ) {
            map.put(LocaleUtil.getSiteDefault(), map.entrySet().iterator().next().getValue());
        } else {
            map.put(LocaleUtil.getSiteDefault(), defString);
        }
    }

    private AssetCategory getCategoryForClass(final OntClass c, final Map<String, AssetCategory> map, long userId, long vocabularyId, ServiceContext serviceContext) throws SystemException, PortalException {
        if ( c.isAnon()) {
            return null;
        }

        AssetCategory c1 = map.get(c.getURI());
        if ( c1 != null ) {
            return c1;
        }

        System.out.println("Adding class " + c.getURI());
        final Map<Locale,String> titleMap = getLangToStringMap(c,RDFS.label.getURI());
        final Map<Locale,String> descriptionMap = getLangToStringMap(c, RDFS.comment.getURI());

        ensureNotEmpty(titleMap, URI.create(c.getURI()).getFragment());
        ensureNotEmpty(descriptionMap, URI.create(c.getURI()).getFragment());

        final List<OntClass> lst = c.listSuperClasses(true).toList();
        System.out.println("  -- SuperClass list size " + lst.size() );

        long parentId = 0;

        if ( lst.size() > 1 ) {
            System.out.println("Multiple inheritance not supported yet, ignoring all parents of " + c.getURI() + ".");
        } else if (lst.size() == 1) {
            c1 = getCategoryForClass(lst.iterator().next(), map, userId, vocabularyId, serviceContext);
            parentId = c1.getCategoryId();
        }

        final AssetCategory cc = AssetCategoryLocalServiceUtil.addCategory(userId, parentId, titleMap, descriptionMap, vocabularyId, new String[]{}, serviceContext);
        map.put(c.getURI(), cc);
        return cc;
//      final String iri = createProperty(c.getIRI().toString());
//      AssetCategoryPropertyLocalServiceUtil.addCategoryProperty(user.getUserId(), assetCategory.getCategoryId(), "iri", iri);
    }

    private String escapeForAssetString(String s) {
        List<Character> cList = new ArrayList<Character>();
        for(char c : INVALID_CHARACTERS) {
            cList.add(c);
        }

        String res="";
        for( char c : s.toCharArray()) {
            int i = cList.indexOf(c);
            if ( i >= 0 ) {
                res += "_"+i+"_";
            } else {
                res += c;
            }
        }
        return res;
    }

    public static final char[] INVALID_CHARACTERS = new char[] {
            CharPool.AMPERSAND, CharPool.APOSTROPHE, CharPool.AT,
            CharPool.BACK_SLASH, CharPool.CLOSE_BRACKET, CharPool.CLOSE_CURLY_BRACE,
            CharPool.COLON, CharPool.COMMA, CharPool.EQUAL, CharPool.GREATER_THAN,
            CharPool.FORWARD_SLASH, CharPool.LESS_THAN, CharPool.NEW_LINE,
            CharPool.OPEN_BRACKET, CharPool.OPEN_CURLY_BRACE, CharPool.PERCENT,
            CharPool.PIPE, CharPool.PLUS, CharPool.POUND, CharPool.PRIME,
            CharPool.QUESTION, CharPool.QUOTE, CharPool.RETURN, CharPool.SEMICOLON,
            CharPool.SLASH, CharPool.STAR, CharPool.TILDE
    };
}
