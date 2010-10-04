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

insert into nom.road_name (road_name_id, language, name)
select nextval('nom.road_name_road_name_id_seq'), 'deu', name
from
(select distinct wt.v as name
from osm.way_tags wt
join nom.feature f
on wt.id = f.source_id 
join nom.link l 
on f.feature_id = l.feature_id
where wt.k = 'name' 
order by wt.v) names;

alter table nom.road_name 
add constraint pk_road_name primary key (road_name_id);
