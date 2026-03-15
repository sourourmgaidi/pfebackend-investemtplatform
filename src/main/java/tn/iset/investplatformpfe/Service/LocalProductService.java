package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.LocalProduct;
import tn.iset.investplatformpfe.Repository.LocalProductRepository;

import java.util.List;

@Service
public class LocalProductService {

    private final LocalProductRepository localProductRepository;

    public LocalProductService(LocalProductRepository localProductRepository) {
        this.localProductRepository = localProductRepository;
    }

    // ========================================
    // CREATE
    // ========================================
    @Transactional
    public LocalProduct createLocalProduct(LocalProduct product) {
        validateProduct(product);
        return localProductRepository.save(product);
    }

    // ========================================
    // READ (GET ALL)
    // ========================================
    public List<LocalProduct> getAllLocalProducts() {
        return localProductRepository.findAll();
    }

    // ========================================
    // READ (GET BY ID)
    // ========================================
    public LocalProduct getLocalProductById(Long id) {
        return localProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Local product not found with id: " + id));
    }

    // ========================================
    // READ (GET BY CATEGORY)
    // ========================================
    public List<LocalProduct> getLocalProductsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("Category is required");
        }
        return localProductRepository.findByCategory(category);
    }

    // ========================================
    // UPDATE
    // ========================================
    @Transactional
    public LocalProduct updateLocalProduct(Long id, LocalProduct productDetails) {
        LocalProduct existingProduct = getLocalProductById(id);

        // Mise à jour des champs
        if (productDetails.getName() != null && !productDetails.getName().trim().isEmpty()) {
            existingProduct.setName(productDetails.getName());
        }

        if (productDetails.getDescription() != null) {
            existingProduct.setDescription(productDetails.getDescription());
        }

        if (productDetails.getCategory() != null) {
            existingProduct.setCategory(productDetails.getCategory());
        }

        return localProductRepository.save(existingProduct);
    }

    // ========================================
    // DELETE
    // ========================================
    @Transactional
    public void deleteLocalProduct(Long id) {
        if (!localProductRepository.existsById(id)) {
            throw new RuntimeException("Local product not found with id: " + id);
        }

        // Vérifier si le produit est utilisé dans des relations ManyToMany
        // Note: La suppression en cascade est gérée par la base de données
        localProductRepository.deleteById(id);
    }

    // ========================================
    // SEARCH
    // ========================================
    public List<LocalProduct> searchLocalProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllLocalProducts();
        }
        return localProductRepository.findByNameContainingIgnoreCase(keyword);
    }

    // ========================================
    // SEARCH BY CATEGORY
    // ========================================
    public List<LocalProduct> searchLocalProductsByCategory(String category, String keyword) {
        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("Category is required");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return getLocalProductsByCategory(category);
        }

        return localProductRepository.findByCategoryAndNameContainingIgnoreCase(category, keyword);
    }

    // ========================================
    // GET ALL CATEGORIES
    // ========================================
    public List<String> getAllCategories() {
        return localProductRepository.findAllCategories();
    }

    // ========================================
    // COUNT BY CATEGORY
    // ========================================
    public Long countByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("Category is required");
        }
        return localProductRepository.countByCategory(category);
    }

    // ========================================
    // GET STATISTICS
    // ========================================
    public Object getStatistics() {
        return localProductRepository.getCategoryStatistics();
    }

    // ========================================
    // BULK CREATE
    // ========================================
    @Transactional
    public List<LocalProduct> createLocalProducts(List<LocalProduct> products) {
        if (products == null || products.isEmpty()) {
            throw new RuntimeException("Products list cannot be empty");
        }

        for (LocalProduct product : products) {
            validateProduct(product);
        }

        return localProductRepository.saveAll(products);
    }

    // ========================================
    // BULK DELETE
    // ========================================
    @Transactional
    public void deleteLocalProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("IDs list cannot be empty");
        }

        List<LocalProduct> products = localProductRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            throw new RuntimeException("Some products were not found");
        }

        localProductRepository.deleteAll(products);
    }

    // ========================================
    // CHECK EXISTENCE
    // ========================================
    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return localProductRepository.existsByName(name);
    }

    // ========================================
    // GET PRODUCTS BY REGION
    // ========================================
    public List<LocalProduct> getProductsByRegionId(Long regionId) {
        if (regionId == null) {
            throw new RuntimeException("Region ID is required");
        }
        return localProductRepository.findByRegionId(regionId);
    }

    // ========================================
    // GET PRODUCTS WITH REGIONS
    // ========================================
    public List<Object[]> getProductsWithRegions() {
        return localProductRepository.findProductsWithRegions();
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateProduct(LocalProduct product) {
        if (product == null) {
            throw new RuntimeException("Product cannot be null");
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new RuntimeException("Local product name is required");
        }

        // Vérifier si le nom existe déjà (optionnel)
        if (localProductRepository.existsByName(product.getName())) {
            throw new RuntimeException("Product with name '" + product.getName() + "' already exists");
        }

        // Validation supplémentaire si nécessaire
        if (product.getName().length() < 2) {
            throw new RuntimeException("Product name must be at least 2 characters long");
        }

        if (product.getName().length() > 100) {
            throw new RuntimeException("Product name must not exceed 100 characters");
        }

        if (product.getDescription() != null && product.getDescription().length() > 500) {
            throw new RuntimeException("Description must not exceed 500 characters");
        }

        if (product.getCategory() != null && product.getCategory().length() > 50) {
            throw new RuntimeException("Category must not exceed 50 characters");
        }
    }
}