package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
}
