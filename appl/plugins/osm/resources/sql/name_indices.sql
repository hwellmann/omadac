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

alter table nom.name_translation
add constraint pk_nametranslation primary key (name_id, translated_name_id);

alter table nom.names
add constraint pk_names primary key (name_id);


alter table nom.feature_name
add constraint pk_featurename primary key (feature_id, name_id);

create index nx_featurename_nameid on nom.feature_name (name_id);


alter table nom.link_admin
add constraint pk_link_admin primary key(link_id, side);

create index linkadmin_adminid on nom.link_admin (admin_id);


alter table nom.road_name
add constraint pk_road_name primary key (road_name_id);

alter table nom.road 
add constraint pk_road primary key (road_id);
