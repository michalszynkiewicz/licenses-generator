/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.snowdrop.licenses.sanitiser;

import org.jboss.snowdrop.licenses.xml.DependencyElement;
import org.jboss.snowdrop.licenses.xml.LicenseElement;

import java.util.Optional;
import java.util.Set;

import static org.jboss.snowdrop.licenses.utils.JsonUtils.loadJsonToSet;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class AliasLicenseSanitiser implements LicenseSanitiser {

    private final Set<RedHatLicense> redHatLicenses;

    private final LicenseSanitiser next;

    public AliasLicenseSanitiser(String namesFilePath, LicenseSanitiser next) {
        this.redHatLicenses = loadJsonToSet(namesFilePath, RedHatLicense::new);
        this.next = next;
    }

    @Override
    public DependencyElement fix(DependencyElement originalDependencyElement) {
        boolean shouldCallNext = false;
        DependencyElement dependencyElement = new DependencyElement(originalDependencyElement);

        for (LicenseElement licenseElement : dependencyElement.getLicenses()) {
            Optional<RedHatLicense> redHatLicenseOptional = getRedHatLicenseByUrl(licenseElement.getUrl());
            if (!redHatLicenseOptional.isPresent()) {
                redHatLicenseOptional = getRedHatLicenseByName(licenseElement.getName());
            }
            if (redHatLicenseOptional.isPresent()) {
                RedHatLicense redHatLicense = redHatLicenseOptional.get();
                licenseElement.setName(redHatLicense.getName());
                licenseElement.setUrl(redHatLicense.getUrl());
            } else {
                shouldCallNext = true;
            }

            if (licenseElement.getTextUrl() == null) {
                licenseElement.setTextUrl(licenseElement.getUrl());
            }
        }

        if (shouldCallNext) {
            return next.fix(dependencyElement);
        }

        return dependencyElement;
    }

    private Optional<RedHatLicense> getRedHatLicenseByUrl(String url) {
        if (url == null) {
            return Optional.empty();
        }
        return redHatLicenses.stream()
                .filter(l -> l.getUrlAliases()
                        .contains(url.toLowerCase()))
                .findFirst();
    }

    private Optional<RedHatLicense> getRedHatLicenseByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return redHatLicenses.stream()
                .filter(l -> l.getAliases()
                        .contains(name.toLowerCase()))
                .findFirst();
    }

}
