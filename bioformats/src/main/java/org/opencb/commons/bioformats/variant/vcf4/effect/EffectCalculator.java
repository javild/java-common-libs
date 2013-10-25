package org.opencb.commons.bioformats.variant.vcf4.effect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.opencb.commons.bioformats.variant.vcf4.VariantEffect;
import org.opencb.commons.bioformats.variant.vcf4.VcfRecord;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aaleman
 * Date: 10/25/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class EffectCalculator {

    public static List<VariantEffect> getEffects(List<VcfRecord> batch) {
        ObjectMapper mapper = new ObjectMapper();
        List<VariantEffect> batchEffect = new ArrayList<>();

        StringBuilder chunkVcfRecords = new StringBuilder();
//        Client wsRestClient = ClientBuilder.newClient();
//        WebTarget wsRestClient = wsRestClient.target("http://ws.bioinfo.cipf.es/cellbase/rest/latest/hsa/genomic/variant/");
        Client wsRestClient = Client.create();
        WebResource webResource = wsRestClient.resource("http://ws.bioinfo.cipf.es/cellbase/rest/latest/hsa/genomic/variant/");

        for (VcfRecord record : batch) {
            chunkVcfRecords.append(record.getChromosome()).append(":");
            chunkVcfRecords.append(record.getPosition()).append(":");
            chunkVcfRecords.append(record.getReference()).append(":");
            chunkVcfRecords.append(record.getAlternate()).append(",");
        }

        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        formDataMultiPart.field("variants", chunkVcfRecords.substring(0, chunkVcfRecords.length() - 1));

//        Response response = webResource.path("consequence_type").queryParam("of", "json").request(MediaType.MULTIPART_FORM_DATA).post(Entity.text(formDataMultiPart.toString()));
        String response = webResource.path("consequence_type").queryParam("of", "json").type(MediaType.MULTIPART_FORM_DATA).post(String.class, formDataMultiPart);

        try {
            batchEffect = mapper.readValue(response.toString(), mapper.getTypeFactory().constructCollectionType(List.class, VariantEffect.class));
        } catch (IOException e) {
            System.err.println(chunkVcfRecords.toString());
            e.printStackTrace();
        }

        return batchEffect;
    }

    public static List<List<VariantEffect>> getEffectPerVariant(List<VcfRecord> batch) {
        List<List<VariantEffect>> list = new ArrayList<>(batch.size());
        List<VariantEffect> auxEffect;
        List<VariantEffect> effects = getEffects(batch);
        VariantEffect effect;

        for (VcfRecord record : batch) {
            auxEffect = new ArrayList<>(20);
            for (int i = 0; i < effects.size(); i++) {
                effect = effects.get(i);
                if (record.getChromosome().equals(effect.getChromosome())
                        && record.getPosition() == effect.getPosition()
                        && record.getReference().equals(effect.getReferenceAllele())
                        && record.getAlternate().equals(effect.getAlternativeAllele())) {
                    auxEffect.add(effect);
                }
            }
            list.add(auxEffect);
        }
        return list;
    }
}
