package com.riigiluup.finance;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartyFinanceImporterTest {

    private final ErjkClient client = mock(ErjkClient.class);
    private final PartyReceiptRepository repo = mock(PartyReceiptRepository.class);
    private final ImportRunLogRepository runLogRepo = mock(ImportRunLogRepository.class);
    private final PartyFinanceImporter importer = new PartyFinanceImporter(
            client, repo, runLogRepo, mock(PlatformTransactionManager.class));

    PartyFinanceImporterTest() {
        when(runLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** An empty ERJK payload must never wipe the table — the run fails, existing rows stay. */
    @Test
    void empty_source_result_keeps_existing_rows_and_marks_run_failed() {
        when(client.fetchAllReceipts()).thenReturn(List.of());

        assertThatThrownBy(importer::importAll).isInstanceOf(IllegalStateException.class);

        verify(repo, never()).deleteAllReceipts();
        ArgumentCaptor<ImportRunLog> captor = ArgumentCaptor.forClass(ImportRunLog.class);
        verify(runLogRepo, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getFinishedAt()).isNotNull();
    }

    @Test
    void non_empty_result_replaces_rows_and_marks_run_success() {
        when(client.fetchAllReceipts()).thenReturn(List.of(
                new ErjkReceiptDto("1234.56", "2024", "1", "Erakond", "2", "Liikmemaksud")));

        assertThat(importer.importAll()).isEqualTo(1);

        verify(repo).deleteAllReceipts();
        ArgumentCaptor<ImportRunLog> captor = ArgumentCaptor.forClass(ImportRunLog.class);
        verify(runLogRepo, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getRecordsUpserted()).isEqualTo(1);
    }
}
