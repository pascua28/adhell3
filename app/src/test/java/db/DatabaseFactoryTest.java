package db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseFactoryTest {

    @Test
    public void findDiffTest() throws IOException {
        File backupFile1 = new File(getClass().getResource("/db/adhell_backup1.txt").getFile());
        File backupFile2 = new File(getClass().getResource("/db/adhell_backup2.txt").getFile());

        List<AppPermissionInfo> contentBackupFile1 = readFile(backupFile1);
        List<AppPermissionInfo> contentBackupFile2 = readFile(backupFile2);

        // Services
        List<AppPermissionInfo> services1 = contentBackupFile1.stream()
                .filter(info -> info.getPermissionStatus() == 2)
                .sorted(Comparator.comparing(AppPermissionInfo::getPackageName))
                .collect(Collectors.toList());

        List<AppPermissionInfo> services2 = contentBackupFile2.stream()
                .filter(info -> info.getPermissionStatus() == 2)
                .sorted(Comparator.comparing(AppPermissionInfo::getPackageName))
                .collect(Collectors.toList());

        System.out.println("Services:");
        List<AppPermissionInfo> serviceDifferences = new ArrayList<>(services1);
        serviceDifferences.removeAll(services2);
        serviceDifferences.forEach(System.out::println);
        System.out.println();

        // Receivers
        List<AppPermissionInfo> receiver1 = contentBackupFile1.stream()
                .filter(info -> info.getPermissionStatus() == 5)
                .sorted(Comparator.comparing(AppPermissionInfo::getPackageName))
                .collect(Collectors.toList());

        List<AppPermissionInfo> receiver2 = contentBackupFile2.stream()
                .filter(info -> info.getPermissionStatus() == 5)
                .sorted(Comparator.comparing(AppPermissionInfo::getPackageName))
                .collect(Collectors.toList());

        System.out.println("Receivers:");
        List<AppPermissionInfo> receiverDifferences = new ArrayList<>(receiver1);
        receiverDifferences.removeAll(receiver2);
        receiverDifferences.forEach(System.out::println);
    }

    @SuppressWarnings("unchecked")
    private List<AppPermissionInfo> readFile(File backupFile) throws IOException {
        if (!backupFile.exists()) {
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(List.class, new AppPermissionDeserializer());
        mapper.registerModule(module);
        return mapper.readValue(backupFile, List.class);
    }
}
