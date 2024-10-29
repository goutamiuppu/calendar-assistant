-- Insert sample employees
INSERT INTO employee (name) VALUES ('John Doe');
INSERT INTO employee (name) VALUES ('Jane Smith');
INSERT INTO employee (name) VALUES ('Bob Johnson');
INSERT INTO employee (name) VALUES ('Alice Williams');

-- Insert sample meetings
INSERT INTO meeting (title, start_time, end_time, owner_id)
VALUES ('Team Meeting', '2023-05-01 10:00:00', '2023-05-01 11:00:00', 1);

INSERT INTO meeting (title, start_time, end_time, owner_id)
VALUES ('Project Discussion', '2023-05-01 14:00:00', '2023-05-01 15:00:00', 2);

INSERT INTO meeting (title, start_time, end_time, owner_id)
VALUES ('Client Call', '2023-05-02 11:00:00', '2023-05-02 12:00:00', 3);

INSERT INTO meeting (title, start_time, end_time, owner_id)
VALUES ('Sprint Planning', '2023-05-03 09:00:00', '2023-05-03 10:30:00', 1);

-- Insert participants for meetings
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (1, 2);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (1, 3);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (2, 1);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (2, 3);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (3, 2);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (3, 4);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (4, 2);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (4, 3);
INSERT INTO meeting_participants (meeting_id, participants_id) VALUES (4, 4);