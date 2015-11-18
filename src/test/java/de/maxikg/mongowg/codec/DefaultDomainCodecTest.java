package de.maxikg.mongowg.codec;

import com.sk89q.worldguard.domains.DefaultDomain;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

public class DefaultDomainCodecTest {

    @Test
    public void testCodec() throws IOException {
        Codec<DefaultDomain> codec = DefaultDomainCodec.INSTANCE;
        DefaultDomain domain = new DefaultDomain();
        domain.addPlayer(UUID.randomUUID());
        domain.addGroup("test_group");

        DefaultDomain other;
        try (StringWriter sw = new StringWriter()) {
            codec.encode(new JsonWriter(sw), domain, EncoderContext.builder().build());
            other = codec.decode(new JsonReader(sw.toString()), DecoderContext.builder().build());
        }

        Assert.assertEquals(domain.getPlayers(), other.getPlayers());
        Assert.assertEquals(domain.getUniqueIds(), other.getUniqueIds());
        Assert.assertEquals(domain.getGroups(), other.getGroups());
    }
}
