package com.universitymanagement.classroom.dto.request;

import java.util.List;
import java.util.UUID;

public record AddStudentsRequest(
        List<UUID> studentIds,

        /**
         * Enrol despite a failed prerequisite or a timetable clash.
         *
         * <p>The rules stay, and the registrar still sees exactly which one was
         * in the way — but they get the final word, because the reasons for
         * overriding are real and the system cannot know them: a transfer
         * student whose earlier study is not in this database, a grade that has
         * not been posted yet, a curriculum entry that is simply wrong.
         *
         * <p>Deleting the checks instead would have removed the explanation
         * along with the block, and there would be nothing left to override.
         */
        boolean override
) {
}
