package org.springframework.content.encryption;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

public class VaultContainerSupport {

    // Testcontainers' VaultContainer no-arg default (hashicorp/vault:1.1.3) is not published under the
    // hashicorp/ namespace, so the image must be pinned explicitly to a valid tag.
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("hashicorp/vault:1.15.6");

    private static VaultContainer vaultContainer = null;

    public static VaultContainer getVaultContainer() {

        if (vaultContainer == null) {
            vaultContainer = new VaultContainer<>(IMAGE_NAME)
                    .withVaultToken("my-root-token")
                    .withVaultPort(8200)
                    .withInitCommand("secrets enable transit");

            vaultContainer.start();
        }

        return vaultContainer;
    }
}
