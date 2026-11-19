package core.club;

/** Stable cursor for million-member clubs; OFFSET pagination is forbidden. */
public record ClubMemberCursor(long clubId, int status, long afterMemberId, int limit) {
    public ClubMemberCursor {
        if (clubId <= 0 || afterMemberId < 0 || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("invalid club member cursor");
        }
    }

    public static ClubMemberCursor firstPage(long clubId, int status, int limit) {
        return new ClubMemberCursor(clubId, status, 0, limit);
    }
}
