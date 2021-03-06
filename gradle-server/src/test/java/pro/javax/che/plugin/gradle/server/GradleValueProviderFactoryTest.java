/*******************************************************************************
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package pro.javax.che.plugin.gradle.server;

import pro.javax.che.plugin.gradle.core.GradleProjectManager;
import pro.javax.che.plugin.gradle.core.connection.internal.DefaultProjectConnectionFactory;

import com.google.common.io.Files;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ValueProvider;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.vfs.server.SystemPathsFilter;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.vfs.impl.fs.LocalFileSystemProvider;
import org.eclipse.che.vfs.impl.fs.WorkspaceHashLocalFSMountStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_BUILD_DIR_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_BUILD_SCRIPT_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_DESCRIPTION_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_GRADLE_VERSION_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_PATH_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_SOURCE_DIR_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_TASKS_VAR;
import static pro.javax.che.plugin.gradle.Constants.PROJECT_TEST_DIR_VAR;

/**
 * @author Vlad Zhukovskyi
 */
public class GradleValueProviderFactoryTest {
    private static final String      workspace     = "my_ws";

    private VirtualFileSystem vfs;

    @Before
    public void setUp() throws Exception {
        final File fsRoot = Files.createTempDir();

        // Clean up after execution
        fsRoot.deleteOnExit();

        final VirtualFileSystemRegistry registry = new VirtualFileSystemRegistry();
        final WorkspaceHashLocalFSMountStrategy mountStrategy = new WorkspaceHashLocalFSMountStrategy(fsRoot, fsRoot);
        final LocalFileSystemProvider vfsProvider = new LocalFileSystemProvider(workspace, mountStrategy,
                                                                                new EventService(), null, SystemPathsFilter.ANY, registry);

        registry.registerProvider("my_vfs", vfsProvider);

        vfs = registry.getProvider("my_vfs").newInstance(URI.create(""));
    }

    @Test
    public void testValueProvider() throws Exception {
        final URL resource = getClass().getClassLoader().getResource("pro/javax/che/plugin/gradle/server/quickstart.zip");

        assertNotNull(resource);

        vfs.importZip(vfs.getMountPoint().getRoot().getId(), resource.openStream(), true, false);

        final VirtualFile project = vfs.getMountPoint().getVirtualFile("/quickstart");

        assertTrue(project.isFolder());

        final FolderEntry projectFolder = new FolderEntry(workspace, project);
        final GradleProjectManager gradleProjectManager = new GradleProjectManager(new DefaultProjectConnectionFactory());
        final ValueProviderFactory valueProviderFactory = new GradleValueProviderFactory(gradleProjectManager);
        final ValueProvider valueProvider = valueProviderFactory.newInstance(projectFolder);

        //Check variables

        List<String> computedPath = valueProvider.getValues(PROJECT_PATH_VAR);
        List<String> expectedPath = singletonList(":");
        assertTrue(elementsEqual(computedPath, expectedPath));

        List<String> computedBuildDir = valueProvider.getValues(PROJECT_BUILD_DIR_VAR);
        List<String> expectedBuildDir = singletonList("build");
        assertTrue(elementsEqual(computedBuildDir, expectedBuildDir));

        List<String> computedBuildScript = valueProvider.getValues(PROJECT_BUILD_SCRIPT_VAR);
        List<String> expectedBuildScript = singletonList("build.gradle");
        assertTrue(elementsEqual(computedBuildScript, expectedBuildScript));

        List<String> computedTasks = valueProvider.getValues(PROJECT_TASKS_VAR);
        List<String> expectedTasks = newArrayList(":assemble", ":build", ":buildDependents", ":buildEnvironment", ":buildNeeded",
                                                  ":check", ":classes", ":clean", ":cleanEclipse", ":components", ":dependencies",
                                                  ":dependencyInsight", ":eclipse", ":help", ":init", ":jar", ":javadoc", ":model",
                                                  ":projects", ":properties", ":tasks", ":test", ":testClasses", ":uploadArchives",
                                                  ":wrapper");
        assertTrue(elementsEqual(computedTasks, expectedTasks));

        List<String> computedDescription = valueProvider.getValues(PROJECT_DESCRIPTION_VAR);
        List<String> expectedDescription = singletonList("");
        assertTrue(elementsEqual(computedDescription, expectedDescription));

        List<String> computedSourceDirectories = valueProvider.getValues(PROJECT_SOURCE_DIR_VAR);
        List<String> expectedSourceDirectories = newArrayList("src/main/resources", "src/main/java");
        assertTrue(elementsEqual(computedSourceDirectories, expectedSourceDirectories));

        List<String> computedTestDirectories = valueProvider.getValues(PROJECT_TEST_DIR_VAR);
        List<String> expectedTestDirectories = newArrayList("src/test/resources", "src/test/java");
        assertTrue(elementsEqual(computedTestDirectories, expectedTestDirectories));

        List<String> computedGradleVersion = valueProvider.getValues(PROJECT_GRADLE_VERSION_VAR);
        List<String> expectedGradleVersion = singletonList("2.10");
        assertTrue(elementsEqual(computedGradleVersion, expectedGradleVersion));
    }

    @Test
    public void testShouldReturnEmptyValueForEmptyProjectFolder() throws Exception {
        vfs.createFolder(vfs.getMountPoint().getRoot().getId(), "empty");
        final VirtualFile project = vfs.getMountPoint().getVirtualFile("/empty");

        assertTrue(project.isFolder());

        final FolderEntry projectFolder = new FolderEntry(workspace, project);
        final GradleProjectManager gradleProjectManager = new GradleProjectManager(new DefaultProjectConnectionFactory());
        final ValueProviderFactory valueProviderFactory = new GradleValueProviderFactory(gradleProjectManager);
        final ValueProvider valueProvider = valueProviderFactory.newInstance(projectFolder);

        List<String> computedPath = valueProvider.getValues(PROJECT_PATH_VAR);
        assertTrue(computedPath.isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        IoUtil.removeDirectory(vfs.getMountPoint().getRoot().getIoFile().getAbsolutePath());
    }
}
