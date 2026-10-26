package org.pipepipe.plugin.tools;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.schabi.newpipe.extractor.plugin.PluginException;
import org.schabi.newpipe.extractor.plugin.PluginManifest;
import org.schabi.newpipe.extractor.plugin.PluginPackage;
import org.schabi.newpipe.extractor.plugin.PluginRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Repository maintenance tool.
 *
 * <pre>
 * genkey  &lt;private-key-file&gt;
 *     Create an Ed25519 key. The file holds the base64 raw private key; keep it secret.
 * index   &lt;private-key-file&gt; &lt;repo-dir&gt; &lt;repo-name&gt;
 *     Sign every *.ppp in repo-dir and write repo-dir/index.json.
 * </pre>
 *
 * Uses the same verification code as the hosts, so a produced index is checked before it
 * is written.
 */
public final class PluginTool {
    private PluginTool() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("genkey")) {
            genKey(new File(args[1]));
        } else if (args.length == 4 && args[0].equals("index")) {
            index(new File(args[1]), new File(args[2]), args[3]);
        } else {
            System.err.println("usage: genkey <key> | index <key> <repo-dir> <repo-name>");
            System.exit(2);
        }
    }

    private static void genKey(final File keyFile) throws IOException {
        if (keyFile.exists()) {
            throw new IOException(keyFile + " already exists, refusing to overwrite");
        }
        final Ed25519KeyPairGenerator generator = new Ed25519KeyPairGenerator();
        generator.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        final AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        final byte[] privateKey = ((Ed25519PrivateKeyParameters) pair.getPrivate()).getEncoded();
        Files.write(keyFile.toPath(), Base64.encode(privateKey));
        try {
            Files.setPosixFilePermissions(keyFile.toPath(),
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (final UnsupportedOperationException ignored) {
            // non posix file system
        }
        System.out.println("public key: " + Base64.toBase64String(
                ((Ed25519PublicKeyParameters) pair.getPublic()).getEncoded()));
    }

    private static void index(final File keyFile, final File repoDir, final String repoName)
            throws IOException, PluginException {
        final Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(
                Base64.decode(new String(Files.readAllBytes(keyFile.toPath()),
                        StandardCharsets.US_ASCII).trim()), 0);
        final String publicKey = Base64.toBase64String(
                privateKey.generatePublicKey().getEncoded());

        final File[] packages = repoDir.listFiles(
                (dir, name) -> name.endsWith(PluginPackage.FILE_EXTENSION));
        if (packages == null || packages.length == 0) {
            throw new IOException("No packages in " + repoDir);
        }
        Arrays.sort(packages);

        final JsonArray plugins = new JsonArray();
        for (final File pkg : packages) {
            final PluginManifest manifest = PluginPackage.readManifest(pkg);
            if (!PluginPackage.hasDex(pkg)) {
                throw new PluginException(pkg.getName() + " has no " + PluginPackage.DEX_ENTRY);
            }
            final byte[] digest = PluginPackage.sha256(pkg);
            final Ed25519Signer signer = new Ed25519Signer();
            signer.init(true, privateKey);
            signer.update(digest, 0, digest.length);
            final String signature = Base64.toBase64String(signer.generateSignature());
            final String sha256 = Hex.toHexString(digest);
            PluginPackage.verify(pkg, sha256, signature, publicKey);

            final JsonObject entry = new JsonObject();
            entry.put("pluginId", manifest.getPluginId());
            entry.put("name", manifest.getName());
            entry.put("versionCode", manifest.getVersionCode());
            entry.put("versionName", manifest.getVersionName());
            entry.put("apiVersion", manifest.getApiVersion());
            entry.put("author", manifest.getAuthor());
            entry.put("package", pkg.getName());
            entry.put("sha256", sha256);
            entry.put("signature", signature);
            entry.put("publicKey", publicKey);
            if (manifest.getDescription() != null) {
                entry.put("description", manifest.getDescription());
            }
            entry.put("nsfw", false);
            plugins.add(entry);
        }

        final JsonObject index = new JsonObject();
        index.put("formatVersion", PluginRepository.FORMAT_VERSION);
        index.put("name", repoName);
        index.put("plugins", plugins);
        final String json = JsonWriter.indent("  ").string().object(index).done();
        // round trip through the host parser
        if (PluginRepository.parse("https://localhost/index.json", json).getPlugins().size()
                != plugins.size()) {
            throw new PluginException("Generated index does not parse");
        }
        Files.write(new File(repoDir, "index.json").toPath(),
                json.getBytes(StandardCharsets.UTF_8));
        System.out.println("indexed " + plugins.size() + " plugins, public key " + publicKey);
    }
}
