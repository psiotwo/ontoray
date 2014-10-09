package cz.sio2.liferay.ontoray;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import org.primefaces.model.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.util.*;

@ManagedBean(name = "bbtaxonomymanager")
@ViewScoped
public class BBTaxonomyManager {

    private static final char[] INVALID_CHARACTERS = new char[] {
            CharPool.AMPERSAND, CharPool.APOSTROPHE, CharPool.AT,
            CharPool.BACK_SLASH, CharPool.CLOSE_BRACKET, CharPool.CLOSE_CURLY_BRACE,
            CharPool.COLON, CharPool.COMMA, CharPool.EQUAL, CharPool.GREATER_THAN,
            CharPool.FORWARD_SLASH, CharPool.LESS_THAN, CharPool.NEW_LINE,
            CharPool.OPEN_BRACKET, CharPool.OPEN_CURLY_BRACE, CharPool.PERCENT,
            CharPool.PIPE, CharPool.PLUS, CharPool.POUND, CharPool.PRIME,
            CharPool.QUESTION, CharPool.QUOTE, CharPool.RETURN, CharPool.SEMICOLON,
            CharPool.SLASH, CharPool.STAR, CharPool.TILDE
    };

    public String escapeForAssetString(String s) {
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


    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    private ConceptProvider p ;

    private String inputType;



    public void upload() {
        if (file != null) {
            final OntModel mdl = ModelFactory.createOntologyModel();

            try {
                mdl.read(file.getInputstream(), null);
                final String testonto = escapeForAssetString("GEN " + DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()));
                transformToCategories(mdl, testonto);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                FacesContext.getCurrentInstance().addMessage(null,new FacesMessage("Error during file processing: '" + e.getMessage() + "'. See stacktrace for details."));
            }
        }
    }

    public void transformToCategories(OntModel o, String testonto) {
        try {
            final ServiceContext serviceContext = LiferayFacesContext.getInstance().getServiceContext();

            User user = PortalUtil.getUser(LiferayFacesContext.getInstance().getPortletRequest());

            if ( user == null ) {
                user = UserLocalServiceUtil.getDefaultUser(PortalUtil.getDefaultCompanyId());
            }

            Map<Locale, String> titleMap = new HashMap<Locale, String>();
            ensureNotEmpty(titleMap,testonto);
            Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
            String settings = "";

            final Map<String,AssetCategory> map = new HashMap<>();

            final AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.addVocabulary(user.getUserId(), testonto, titleMap, descriptionMap, settings, serviceContext);

            for(OntResource c : p.getConcepts(o)) {
                getCategoryForConcept(c, map, user.getUserId(), vocabulary.getVocabularyId(), serviceContext);
            }
        } catch (PortalException | SystemException e) {
            e.printStackTrace();
        }
    }

    private void ensureNotEmpty( final Map<Locale, String> map, final String defString) {
        if (map.keySet().size() > 0 ) {
            map.put(LocaleUtil.getSiteDefault(), map.entrySet().iterator().next().getValue());
        } else {
            map.put(LocaleUtil.getSiteDefault(), defString);
        }
    }

    private String cutString(String s, int l) {
        return s.substring(0,Math.min(l,s.length()));
    }

    private AssetCategory getCategoryForConcept(final OntResource c, final Map<String, AssetCategory> map, long userId, long vocabularyId, ServiceContext serviceContext) throws SystemException, PortalException {
        if ( c.isAnon()) {
            return null;
        }

        AssetCategory c1 = map.get(c.getURI());
        if ( c1 != null ) {
            return c1;
        }

        final Concept concept = p.getConcept(c);

        System.out.println("Adding class " + c.getURI());

        final Map<Locale,String> titleMap = concept.getLabel();
        ensureNotEmpty(titleMap, URI.create(c.getURI()).getFragment());

        final Map<Locale,String> descriptionMap = concept.getDescription();
        ensureNotEmpty(descriptionMap, URI.create(c.getURI()).getFragment());

        final List<String> categoryProperties = new ArrayList<>();
        for (final String key : concept.getProperties().keySet()) {
            categoryProperties.add(cutString(escapeForAssetString(key),75)+":"+cutString(escapeForAssetString(concept.getProperties().get(key)),75));
        }

        final List<? extends OntResource> lst = p.getMoreGeneralConcepts(c);
        System.out.println("  -- SuperClass list size " + lst.size() );

        long parentId = 0;

        if ( lst.size() > 1 ) {
            System.out.println("Multiple inheritance not supported yet, ignoring all parents of " + c.getURI() + ".");
        } else if (lst.size() == 1) {
            c1 = getCategoryForConcept(lst.iterator().next(), map, userId, vocabularyId, serviceContext);
            parentId = c1.getCategoryId();
        }

        final AssetCategory cc = AssetCategoryLocalServiceUtil.addCategory(userId, parentId, titleMap, descriptionMap, vocabularyId, categoryProperties.toArray(new String[]{}), serviceContext);
        map.put(c.getURI(), cc);
        return cc;
//      final String iri = createProperty(c.getIRI().toString());
//      AssetCategoryPropertyLocalServiceUtil.addCategoryProperty(user.getUserId(), assetCategory.getCategoryId(), "iri", iri);
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
        if ( "skos".equals(inputType)) {
            p = new ConceptProviderSKOSTerminology();
        } else {
            p = new ConceptProviderOWLClass();
        }
    }
}
