package aisafe.infrastructure.persistence.jpa;

import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaCollaboratorRepository
        extends JpaAutoTxRepository<Collaborator, Long, Long>
        implements CollaboratorRepository {

    public JpaCollaboratorRepository(final String puName) {
        super(puName, "id");
    }
}
