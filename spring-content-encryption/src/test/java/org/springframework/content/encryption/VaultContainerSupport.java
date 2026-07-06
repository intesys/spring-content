package org.springframework.content.encryption;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

public class VaultContainerSupport {

    private static VaultContainer vaultContainer = null;

    public static VaultContainer getVaultContainer() {

        if (vaultContainer == null) {
            vaultContainer = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.13.3"))
                    .withVaultToken("my-root-token")
                    .withVaultPort(8200)
                    .withInitCommand("secrets enable transit");

            vaultContainer.start();
        }

        return vaultContainer;
    }
}
