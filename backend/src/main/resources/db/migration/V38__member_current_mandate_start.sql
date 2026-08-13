-- Start date of an MP's current (open-ended) mandate this term. For members who have served since
-- the term opened this equals the term start; for a substitute / by-election entrant it is when they
-- actually took the seat mid-term. Sourced from the Riigikogu detail API (memberships[].
-- membershipRoleItems[] with endDate = null). Nullable; populated by the member-detail import.
ALTER TABLE plenary_member ADD COLUMN current_mandate_start DATE;
