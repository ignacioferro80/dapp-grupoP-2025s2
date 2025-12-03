package predictions.dapp.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.model.ApiKey;
import predictions.dapp.model.Consultas;
import predictions.dapp.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DataJpaTest
@ActiveProfiles("test")
class RepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultasRepository consultasRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        consultasRepository.deleteAll();
        apiKeyRepository.deleteAll();
    }

    @Tag("unit")
    @Test
    void testUserRepository_SaveAndFindByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPasswordHash("hashedpassword");

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("testuser", found.get().getUsername());
    }

    @Tag("unit")
    @Test
    void testUserRepository_FindByEmail_NotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testUserRepository_ExistsByEmail_True() {
        User user = new User();
        user.setEmail("exists@example.com");
        user.setUsername("existsuser");
        user.setPasswordHash("hash");

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("exists@example.com");

        assertTrue(exists);
    }

    @Tag("unit")
    @Test
    void testUserRepository_ExistsByEmail_False() {
        boolean exists = userRepository.existsByEmail("doesnotexist@example.com");

        assertFalse(exists);
    }

    @Tag("unit")
    @Test
    void testUserRepository_SaveMultipleUsers() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setUsername("user1");
        user1.setPasswordHash("hash1");

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setUsername("user2");
        user2.setPasswordHash("hash2");

        userRepository.save(user1);
        userRepository.save(user2);

        assertEquals(2, userRepository.count());
    }

    @Tag("unit")
    @Test
    void testUserRepository_DeleteUser() {
        User user = new User();
        user.setEmail("delete@example.com");
        user.setUsername("deleteuser");
        user.setPasswordHash("hash");

        User saved = userRepository.save(user);
        userRepository.delete(saved);

        Optional<User> found = userRepository.findByEmail("delete@example.com");
        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testUserRepository_UpdateUser() {
        User user = new User();
        user.setEmail("update@example.com");
        user.setUsername("originalname");
        user.setPasswordHash("hash");

        User saved = userRepository.save(user);
        saved.setUsername("updatedname");
        userRepository.save(saved);

        Optional<User> found = userRepository.findByEmail("update@example.com");
        assertTrue(found.isPresent());
        assertEquals("updatedname", found.get().getUsername());
    }

    @Tag("unit")
    @Test
    void testUserRepository_SaveUserWithNullPassword() {
        User user = new User();
        user.setEmail("nullpass@example.com");
        user.setUsername("nullpassuser");
        user.setPasswordHash(null);

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNull(saved.getPasswordHash());
    }

    @Tag("unit")
    @Test
    void testUserRepository_FindByEmailCaseSensitive() {
        User user = new User();
        user.setEmail("CaseSensitive@example.com");
        user.setUsername("caseuser");
        user.setPasswordHash("hash");

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("casesensitive@example.com");
        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_SaveAndFindByUserId() {
        Consultas consultas = new Consultas();
        consultas.setUserId(1L);
        consultas.setPredicciones("[{\"prediction\":\"test\"}]");
        consultas.setRendimiento("[{\"performance\":\"test\"}]");

        consultasRepository.save(consultas);

        Optional<Consultas> found = consultasRepository.findByUserId(1L);

        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getUserId());
        assertNotNull(found.get().getPredicciones());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_FindByUserId_NotFound() {
        Optional<Consultas> found = consultasRepository.findByUserId(999L);

        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_UpdateConsultas() {
        Consultas consultas = new Consultas();
        consultas.setUserId(2L);
        consultas.setPredicciones("[]");

        Consultas saved = consultasRepository.save(consultas);
        saved.setPredicciones("[{\"new\":\"data\"}]");
        consultasRepository.save(saved);

        Optional<Consultas> found = consultasRepository.findByUserId(2L);
        assertTrue(found.isPresent());
        assertTrue(found.get().getPredicciones().contains("new"));
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_SaveWithNullFields() {
        Consultas consultas = new Consultas();
        consultas.setUserId(3L);
        consultas.setPredicciones(null);
        consultas.setRendimiento(null);

        Consultas saved = consultasRepository.save(consultas);

        assertNotNull(saved.getId());
        assertEquals(3L, saved.getUserId());
        assertNull(saved.getPredicciones());
        assertNull(saved.getRendimiento());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_DeleteConsultas() {
        Consultas consultas = new Consultas();
        consultas.setUserId(4L);

        Consultas saved = consultasRepository.save(consultas);
        consultasRepository.delete(saved);

        Optional<Consultas> found = consultasRepository.findByUserId(4L);
        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_SaveMultipleForDifferentUsers() {
        Consultas consultas1 = new Consultas();
        consultas1.setUserId(5L);
        consultas1.setPredicciones("[]");

        Consultas consultas2 = new Consultas();
        consultas2.setUserId(6L);
        consultas2.setPredicciones("[]");

        consultasRepository.save(consultas1);
        consultasRepository.save(consultas2);

        assertEquals(2, consultasRepository.count());
        assertTrue(consultasRepository.findByUserId(5L).isPresent());
        assertTrue(consultasRepository.findByUserId(6L).isPresent());
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_UpdateRendimiento() {
        Consultas consultas = new Consultas();
        consultas.setUserId(7L);
        consultas.setRendimiento("[{\"old\":\"data\"}]");

        Consultas saved = consultasRepository.save(consultas);
        saved.setRendimiento("[{\"updated\":\"performance\"}]");
        consultasRepository.save(saved);

        Optional<Consultas> found = consultasRepository.findByUserId(7L);
        assertTrue(found.isPresent());
        assertTrue(found.get().getRendimiento().contains("updated"));
    }

    @Tag("unit")
    @Test
    void testConsultasRepository_SaveWithLongJsonData() {
        Consultas consultas = new Consultas();
        consultas.setUserId(8L);
        StringBuilder longJson = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            longJson.append("{\"item\":\"").append(i).append("\"}");
            if (i < 99) {
                longJson.append(",");
            }
        }
        longJson.append("]");
        consultas.setPredicciones(longJson.toString());

        Consultas saved = consultasRepository.save(consultas);

        assertNotNull(saved.getId());
        assertTrue(saved.getPredicciones().length() > 1000);
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_SaveAndFindByKey() {
        User user = new User();
        user.setEmail("apitest@example.com");
        user.setUsername("apiuser");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("test-api-key-123");
        apiKey.setUser(savedUser);

        apiKeyRepository.save(apiKey);

        Optional<ApiKey> found = apiKeyRepository.findByKey("test-api-key-123");

        assertTrue(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_FindByKey_NotFound() {
        Optional<ApiKey> found = apiKeyRepository.findByKey("nonexistent-key");

        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_SaveMultipleKeys() {
        User user1 = new User();
        user1.setEmail("user1api@example.com");
        user1.setUsername("user1api");
        user1.setPasswordHash("hash1");
        User savedUser1 = userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("user2api@example.com");
        user2.setUsername("user2api");
        user2.setPasswordHash("hash2");
        User savedUser2 = userRepository.save(user2);

        ApiKey key1 = new ApiKey();
        key1.setKey("key-1");
        key1.setUser(savedUser1);

        ApiKey key2 = new ApiKey();
        key2.setKey("key-2");
        key2.setUser(savedUser2);

        apiKeyRepository.save(key1);
        apiKeyRepository.save(key2);

        assertEquals(2, apiKeyRepository.count());
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_DeleteApiKey() {
        User user = new User();
        user.setEmail("deleteapi@example.com");
        user.setUsername("deleteapiuser");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("delete-key");
        apiKey.setUser(savedUser);

        ApiKey saved = apiKeyRepository.save(apiKey);
        apiKeyRepository.delete(saved);

        Optional<ApiKey> found = apiKeyRepository.findByKey("delete-key");
        assertFalse(found.isPresent());
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_UpdateApiKey() {
        User user = new User();
        user.setEmail("updateapi@example.com");
        user.setUsername("updateapiuser");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("update-key");
        apiKey.setUser(savedUser);

        ApiKey saved = apiKeyRepository.save(apiKey);

        Optional<ApiKey> found = apiKeyRepository.findByKey("update-key");
        assertTrue(found.isPresent());
    }



    @Tag("unit")
    @Test
    void testApiKeyRepository_SaveWithSpecialCharacters() {
        User user = new User();
        user.setEmail("special@example.com");
        user.setUsername("specialuser");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("special-key-!@#$%^&*()");
        apiKey.setUser(savedUser);

        ApiKey saved = apiKeyRepository.save(apiKey);

        Optional<ApiKey> found = apiKeyRepository.findByKey("special-key-!@#$%^&*()");
        assertTrue(found.isPresent());
    }


    @Tag("unit")
    @Test
    void testApiKeyRepository_FindAllKeys() {
        User user1 = new User();
        user1.setEmail("alpha@example.com");
        user1.setUsername("alphauser");
        user1.setPasswordHash("hash");
        User savedUser1 = userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("beta@example.com");
        user2.setUsername("betauser");
        user2.setPasswordHash("hash");
        User savedUser2 = userRepository.save(user2);

        ApiKey key1 = new ApiKey();
        key1.setKey("key-alpha");
        key1.setUser(savedUser1);

        ApiKey key2 = new ApiKey();
        key2.setKey("key-beta");
        key2.setUser(savedUser2);

        apiKeyRepository.save(key1);
        apiKeyRepository.save(key2);

        List<ApiKey> allKeys = (List<ApiKey>) apiKeyRepository.findAll();
        assertEquals(2, allKeys.size());
    }

    @Tag("unit")
    @Test
    void testApiKeyRepository_UserRelationship() {
        User user = new User();
        user.setEmail("relationship@example.com");
        user.setUsername("relationshipuser");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("relationship-key");
        apiKey.setUser(savedUser);

        apiKeyRepository.save(apiKey);

        Optional<ApiKey> found = apiKeyRepository.findByKey("relationship-key");
        assertTrue(found.isPresent());
    }
}