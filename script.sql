CREATE TABLE CAR( NAME TEXT (255),ID INTEGER PRIMARY KEY)
CREATE TABLE DRIVER( NAME TEXT (255),ID INTEGER PRIMARY KEY)

ALTER TABLE CAR ADD DRIVER INTEGER REFERENCES DRIVER(ID)
