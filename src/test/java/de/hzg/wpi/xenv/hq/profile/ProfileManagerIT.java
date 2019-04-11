package de.hzg.wpi.xenv.hq.profile;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.nio.file.Paths;

import static de.hzg.wpi.xenv.hq.HeadQuarter.PROFILES_ROOT;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 4/11/19
 */
public class ProfileManagerIT {
    @Test
    public void testCopyDefaultProfile() throws Exception {
        Profile newTestProfile = new Profile("newTestProfile", null, null, null);

        ProfileManager manager = new ProfileManager();

        manager.executeAnt(newTestProfile, "copy-profile");
        FileUtils.forceDeleteOnExit(Paths.get(PROFILES_ROOT).resolve(newTestProfile.name).toFile());
    }

    @Test
    public void testCreateDeleteProfile() throws Exception {
        Profile newTestProfile = new Profile("newTestProfile", null, null, null);

        ProfileManager manager = new ProfileManager();

        manager.createProfile("newTestProfile", "", "", null);

        Thread.sleep(3000);

        manager.deleteProfile("newTestProfile");

        FileUtils.forceDeleteOnExit(Paths.get(PROFILES_ROOT).resolve(newTestProfile.name).toFile());
    }

}