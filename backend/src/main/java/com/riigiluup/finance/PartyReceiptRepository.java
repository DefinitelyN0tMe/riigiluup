package com.riigiluup.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PartyReceiptRepository extends JpaRepository<PartyReceipt, UUID> {

    /** Bulk delete (immediate) so a full refresh's inserts don't collide with old rows. */
    @Modifying
    @Query("delete from PartyReceipt")
    void deleteAllReceipts();
}
