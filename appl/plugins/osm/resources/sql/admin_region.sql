--  Omadac - The Open Map Database Compiler
--  http://omadac.org
--
--  (C) 2010, Harald Wellmann and Contributors
--
--  This library is free software; you can redistribute it and/or
--  modify it under the terms of the GNU Lesser General Public
--  License as published by the Free Software Foundation;
--  version 2.1 of the License.
--
--  This library is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
--  Lesser General Public License for more details.

-- Deutschland
insert into nom.admin_region 
(feature_id, admin_order, order0_id)
values (1, 0, 1);

insert into nom.feature_name 
(feature_id, feature_class, name_id, name_type)
VALUES (1, 'A', 1, 'E');

insert into nom.names
(name_id, language, name)
values (1, 'ger', 'Deutschland');

-- Bundesland Hamburg
insert into nom.admin_region 
(feature_id, admin_order, order0_id, order1_id)
values (2, 1, 1, 2);

insert into nom.feature_name 
(feature_id, feature_class, name_id, name_type)
VALUES (2, 'A', 2, 'E');

insert into nom.names
(name_id, language, name)
values (2, 'GER', 'Hamburg');

-- Stadt Hamburg
insert into nom.admin_region 
(feature_id, admin_order, order0_id, order1_id, order8_id)
values (3, 8, 1, 2, 3);

insert into nom.feature_name 
(feature_id, feature_class, name_id, name_type)
values (3, 'A', 2, 'E');


alter table nom.admin_region
add constraint pk_admin_region primary key (feature_id);


