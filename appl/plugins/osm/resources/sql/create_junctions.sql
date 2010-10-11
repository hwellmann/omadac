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

-- Create temporary table with node IDs of all start and end nodes 
-- of highways and all nodes belonging to at least two highways.

create temporary table tmp_node as
  select node_id from osm.way_nodes wn
  join osm.way_tags wt
  on wn.id = wt.id and wt.k = 'highway'
  where sequence_id = 0
union
  select wn.node_id from osm.way_nodes wn 
  join osm.way_tags wt
  on wn.id = wt.id and wt.k = 'highway'
  join 
    (select id, max(sequence_id) as seq_num from osm.way_nodes
     group by id) wn2
  on wn.id = wn2.id and wn.sequence_id = wn2.seq_num
union
  select wn.node_id from osm.way_nodes wn
  join osm.way_tags wt
  on wn.id = wt.id and wt.k = 'highway'
  group by wn.node_id
  having count(wn.id) > 1;


-- Create junctions for the node IDs collected above, using the elevation
-- and layer tags as z-coordinate and z-level, respectively.  
  
insert into nom.feature (feature_id, feature_type, discriminator, source_id)
select nextval('nom.feature_feature_id_seq') as feature_id, 
  ft.feature_type as feature_type,
  'J' as discriminator,
  n.id as source_id
from osm.nodes n
join nom.nom_feature_type ft
  on ft.description = 'POINT_JUNCTION'
join tmp_node nn
  on n.id = nn.node_id;

insert into nom.junction  
select f.feature_id as feature_id,
  n.longitude as x,
  n.latitude as y,
  ele.v::float::int as z, 
  l.v::smallint as z_level 
from osm.nodes n
join nom.feature f
  on n.id = f.source_id
join tmp_node nn
  on n.id = nn.node_id
left join osm.node_tags ele
  on n.id = ele.id and ele.k = 'ele'
left join osm.node_tags l
  on n.id = l.id and l.k = 'layer';

drop table tmp_node;
