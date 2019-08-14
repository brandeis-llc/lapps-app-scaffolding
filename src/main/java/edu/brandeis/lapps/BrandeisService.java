package edu.brandeis.lapps;


import org.lappsgrid.api.WebService;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.Contains;
import org.lappsgrid.serialization.lif.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.lappsgrid.discriminator.Discriminators.Uri;

public abstract class BrandeisService implements WebService {

    public static final String containerJsonScheme = "http://vocab.lappsgrid.org/schema/1.1.0/lif-schema.json";
    public static final String metadataJsonScheme = "http://vocab.lappsgrid.org/schema/1.1.0/metadata-schema.json";
    public static final String TOKEN_ID = "tk_";
    public static final String SENT_ID = "s_";
    public static final String CONSTITUENT_ID = "c_";
    public static final String PS_ID = "ps_";
    public static final String DEPENDENCY_ID = "dep_";
    public static final String DS_ID = "ds_";
    public static final String MENTION_ID = "m_";
    public static final String COREF_ID = "coref_";
    public static final String NE_ID = "ne_";

    static final Map<String, String> tagsetMap;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put(Uri.POS, "posTagSet");
        aMap.put(Uri.NE, "namedEntityCategorySet");
        aMap.put(Uri.PHRASE_STRUCTURE, "categorySet");
        aMap.put(Uri.DEPENDENCY_STRUCTURE, "dependencySet");
        tagsetMap = Collections.unmodifiableMap(aMap);
    }

    protected ServiceMetadata metadata;

    /**
     * Default constructor only tries to load metadata.
     * Doing this will also set up metadata and keep it in memory
     */
    protected BrandeisService() {
        try {
            this.metadata = loadMetadata();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*** LAPPS Grid methods ***/

    /**
     * This is default execute: takes a json, wrap it as a LIF, run individual tool
     */
    @Override
    public String execute(String input) {
        if (input == null)
            return null;
        // in case of Json
        Data data;

        try {
            data = Serializer.parse(input, Data.class);
            // Serializer#parse throws JsonParseException if input is not well-formed
        } catch (Exception e) {
            data = new Data();
            data.setDiscriminator(Uri.TEXT);
            data.setPayload(input);
        }

        final String discriminator = data.getDiscriminator();
        Container payload;

        switch (discriminator) {
            case Uri.ERROR:
                return input;
            case Uri.JSON_LD:
            case Uri.LIF:
                payload = new Container((Map) data.getPayload());
                // TODO: 5/9/18 what if the existing payload has different schema version?
                break;
            case Uri.TEXT:
                payload = new Container();
                // TODO: 5/9/18  fix url when it settles in
                payload.setSchema(containerJsonScheme);
                payload.setText((String) data.getPayload());
                payload.setLanguage("en");
                break;
            default:
                String message = String.format
                        ("Unsupported discriminator type: %s", discriminator);
                return new Data<>(Uri.ERROR, message).asJson();
        }

        try {
            return processPayload(payload);
        } catch (Throwable th) {
            th.printStackTrace();
            String message =
                    String.format("Error processing input: %s", th.toString());
            return new Data<>(Uri.ERROR, message).asJson();
        }
    }

    @Override
    public String getMetadata() {
        return new Data<>(Uri.META, this.metadata).asJson();
    }

    /*** Helper methods ***/

    private String getVersion(String versionKey) {
        String path = "/version.properties";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            return "UNKNOWN";
        }
        Properties properties = new Properties();
        try {
            properties.load(stream);
            stream.close();
            return (String) properties.get(versionKey);
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }

    public String getWrappeeVersion() {
        return getVersion("toolversion");
    }

    public String getWrapperVersion() {
        return getVersion("version");
    }


    protected void setUpContainsMetadata(View view, String producerAlias) {
        for (String atype : this.metadata.getProduces().getAnnotations()) {
            Contains newContains = view.addContains(atype,
                    String.format("%s:%s", this.getClass().getName(), getWrapperVersion()),
                    getContainsType(atype, producerAlias));
            if (this.metadata.getProduces().getTagSets().containsKey(atype)) {
                newContains.put(tagsetMap.get(atype),
                        this.metadata.getProduces().getTagSets().get(atype));
            }
        }
    }

    /**
     * Set up a metadata scaffolding for individual services.
     * For minimal set-up, values are set for:
     * <ul>
     *   <li>JSON schema URI</li>
     *   <li>vender</li>
     *   <li>license (apache2)</li>
     *   <li>version (auto-retrieved from pom)</li>
     *   <li>and name (after the service class name)
     * </ul>
     * @return a ServiceMetadata instance with minimal specification
     */
    protected ServiceMetadata setDefaultMetadata() {
        ServiceMetadata commonMetadata = new ServiceMetadata();
        // TODO: 4/22/18 fix url when it settles in
        commonMetadata.setSchema(metadataJsonScheme);
        commonMetadata.setVendor("http://www.cs.brandeis.edu/");
        commonMetadata.setLicense(Uri.APACHE2);
        commonMetadata.setVersion(this.getWrapperVersion());
        commonMetadata.setToolVersion(this.getWrappeeVersion());
        commonMetadata.setName(this.getClass().getName());

        return commonMetadata;
    }

    private static String getContainsType(String aType, String producerAlias) {
        String serviceType;
        switch(aType) {
            case(Uri.TOKEN):
                serviceType = "tokenizer";
                break;
            case(Uri.SENTENCE):
                serviceType = "splitter";
                break;
            case(Uri.POS):
                serviceType = "postagger";
                break;
            case(Uri.NE):
                serviceType = "ner";
                break;
            case(Uri.COREF):
                serviceType = "coreference";
                break;
            case(Uri.MARKABLE):
                serviceType = "markable";
                break;
            case(Uri.CONSTITUENT):
                serviceType = "syntacticparser";
                break;
            case(Uri.PHRASE_STRUCTURE):
                serviceType = "syntacticparser";
                break;
            case(Uri.DEPENDENCY):
                serviceType = "dependency-parser";
                break;
            case(Uri.DEPENDENCY_STRUCTURE):
                serviceType = "dependency-parser";
                break;
            default:
                serviceType = "UNKNOWN";
        }
        return String.format("%s:%s:%s", serviceType, producerAlias, "brandeis");

    }

    protected static String shortenAType(String aType) {
        return aType.substring(aType.lastIndexOf('/') + 1);
    }

    protected String unmetRequirements(String notFound) {
        return String.format("Unsupported input! %s expects %s annotations, but cannot find %s in previous views.",
                this.getClass().getSimpleName(),
                metadata.getRequires().getAnnotations().stream().map(BrandeisService::shortenAType).collect(Collectors.toList()).toString(),
                shortenAType(notFound));
    }

    /** These will be overridden for each individual service **/
    protected abstract String processPayload(Container json) throws BrandeisServiceException;

    protected abstract ServiceMetadata loadMetadata();

    // TODO: 3/11/2019 POJO metadata must be read-only from the world
    public ServiceMetadata getMetadataPojo() {
        return this.metadata;
    }

}

