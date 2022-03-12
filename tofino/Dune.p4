#include "common/constants.p4"
#include "common/intrinsic_metadata.p4"
#include "common/primitives.p4"
#include "common/stateful_alu_blackbox.p4"

#include "common/header_and_parser.p4"
//column number
#define ARRAY_COL_LEN 8192
#define COOKIE_COL_LEN 8192
//the index bit to find cookie pos: which is 2 ^ COOKIE_INDEX_LEN = COOKIE_COL_LEN
#define COOKIE_INDEX_LEN 13
#define COOKIE_OFFSET_LEN 3

header_type user_metadata_t
{
    fields
    {
        //used for inserting
        row0_address: COOKIE_INDEX_LEN;
        row0_offset: 8; //for normal case, used for recording the inserting offset
        row0_satisfy_cell_offset: 8; // for INT case, used for recording the satified cell's offset of row0
        
        row1_address: COOKIE_INDEX_LEN;
        row1_offset: 8;
        row1_satisfy_cell_offset: 8;
        
        //used for taking
        absAddress: COOKIE_INDEX_LEN; //the absoulute address, the bit num is 13 because it needs to be 8-bytes aligned

        //if this packet is INT packet
        is_INT: 1;

        row0_value: 32; //value from row1 
        row1_value: 32; //value from row2
    
        //for threshold, mask in another aspact
        cookie_threshold: 8;
        cookie_hit0: 8; //the current cookie meets the selected requirment
        cookie_hit1: 8;

        tmp: 16;
    }
}
metadata user_metadata_t m;


field_list flow_id_list1{
    ipv4.proto;
    ipv4.sip;
    ipv4.dip;
    udp.sPort;
    udp.dPort;
}

field_list_calculation hash_1_for_address {
    input {flow_id_list1;}
    algorithm: crc16;
    output_width: COOKIE_INDEX_LEN;
}

field_list_calculation hash_1_for_offset {
    input {flow_id_list1;}
    algorithm: crc16;
    output_width: 8;
}

field_list_calculation hash_2_for_address {
    input {flow_id_list1;}
    algorithm: crc8;
    output_width: COOKIE_INDEX_LEN;
}

field_list_calculation hash_2_for_offset {
    input {flow_id_list1;}
    algorithm: crc8;
    output_width: 8;
}



/****   using for threshold, mask in other word     ***/

action act_get_dune_threshold(threshold)
{
    m.cookie_threshold = threshold;
}

table tbl_get_dune_threshold
{
    reads
    {
        m.is_INT: exact;
    }
    actions
    {
        act_get_dune_threshold;
    }
    default_action: act_get_dune_threshold;
    size: 1;
}


action getHashIndex0()
{
    
    modify_field_with_hash_based_offset(m.row0_address,0,hash_1_for_address,ARRAY_COL_LEN);
    modify_field_with_hash_based_offset(m.row0_offset,0,hash_1_for_offset,8);
}

/**
    get two insertion hash index by flow id
*/
table tbl_getHashIndex0
{
    actions 
    {
        getHashIndex0;
    }
    default_action: getHashIndex0;
    size:1;
}

action getHashIndex1()
{
    
	modify_field_with_hash_based_offset(m.row1_address,0,hash_2_for_address,ARRAY_COL_LEN);
    modify_field_with_hash_based_offset(m.row1_offset,0,hash_2_for_offset,8);
    
}

/**
    get two insertion hash index by flow id
*/
table tbl_getHashIndex1
{
    actions 
    {
        getHashIndex1;
    }
    default_action: getHashIndex1;
    size:1;
}

action getRandValue()
{
    modify_field_rng_uniform(m.absAddress, 0, 8191);
}

table tbl_getRandValue
{
    actions
    {
        getRandValue;
    }
    default_action: getRandValue;
    size: 1;
}


action act_modify_address()
{
    modify_field(m.row0_address, m.absAddress);
    modify_field(m.row1_address, m.absAddress);
    modify_field(m.row0_offset, 0);
    modify_field(m.row1_offset, 0);
}

table tbl_modify_address
{
    actions
    {
        act_modify_address;
    }
    default_action: act_modify_address;
}

/**
    forwarding table: get egress_port by matching dst ip exactly
*/
action forward(egress_port)
{
    modify_field(ig_intr_md_for_tm.ucast_egress_port, egress_port);
}

table tbl_forward
{
    reads
    {
        ipv4.dip: exact;
    }
    actions
    {
        forward;
    }
    default_action:forward;
    size:32;
}


action set_is_INT()
{
    modify_field(m.is_INT, 1);
}
table tbl_set_is_INT
{
    actions
    {
        set_is_INT;
    }
    default_action: set_is_INT;
    size:1;
}



action act_write()
{
    modify_field(m.tmp, m.absAddress);
    //shift_left(m.tmp, m.tmp, COOKIE_OFFSET_LEN);
    modify_field(Scatter_s.absAddress, m.tmp);
    modify_field(Scatter_s.offset0, m.row0_offset);
    modify_field(Scatter_s.offset1, m.row1_offset);
    modify_field(Scatter_s.value0, m.row0_value);
    modify_field(Scatter_s.value1, m.row1_value);

    //for debug
}

table take_t
{
    
    actions
    {
        act_write;
    }
    default_action: act_write;
}


action act_no_action()
{

}

/*********************  array a_0_0  *************************/
register a_0_0
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_0
{
    reg: a_0_0;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_0
{
    reg: a_0_0;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_0()
{
    b_insert_a_0_0.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_0()
{
    b_get_a_0_0.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 0: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_0
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_0;
        act_get_a_0_0;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_1  *************************/
register a_0_1
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_1
{
    reg: a_0_1;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_1
{
    reg: a_0_1;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_1()
{
    //b_insert_a_0_1.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_1()
{
    b_get_a_0_1.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 1: do insert
    is_INT == 1 && row0_offset == 1: do get
*/
table tbl_a_0_1
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_1;
        act_get_a_0_1;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_2  *************************/
register a_0_2
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_2
{
    reg: a_0_2;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_2
{
    reg: a_0_2;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_2()
{
    b_insert_a_0_2.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_2()
{
    b_get_a_0_2.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 2: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_2
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_2;
        act_get_a_0_2;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_3  *************************/
register a_0_3
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_3
{
    reg: a_0_3;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_3
{
    reg: a_0_3;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_3()
{
    b_insert_a_0_3.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_3()
{
    b_get_a_0_3.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 3: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_3
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_3;
        act_get_a_0_3;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_4  *************************/
register a_0_4
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_4
{
    reg: a_0_4;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_4
{
    reg: a_0_4;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_4()
{
    b_insert_a_0_4.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_4()
{
    b_get_a_0_4.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 4: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_4
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_4;
        act_get_a_0_4;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_5  *************************/
register a_0_5
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_5
{
    reg: a_0_5;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_5
{
    reg: a_0_5;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_5()
{
    b_insert_a_0_5.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_5()
{
    b_get_a_0_5.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 5: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_5
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_5;
        act_get_a_0_5;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_6  *************************/
register a_0_6
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_6
{
    reg: a_0_6;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_6
{
    reg: a_0_6;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_6()
{
    b_insert_a_0_6.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_6()
{
    b_get_a_0_6.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 6: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_6
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_6;
        act_get_a_0_6;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_0_7  *************************/
register a_0_7
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_0_7
{
    reg: a_0_7;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_0_7
{
    reg: a_0_7;
    output_value: register_lo;
    output_dst: m.row0_value;
}

action act_insert_a_0_7()
{
    b_insert_a_0_7.execute_stateful_alu(m.row0_address);
}

action act_get_a_0_7()
{
    b_get_a_0_7.execute_stateful_alu(m.row0_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row0_offset == 7: do insert
    is_INT == 1 && row0_offset == 0: do get
*/
table tbl_a_0_7
{
    reads
    {
        m.is_INT: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_insert_a_0_7;
        act_get_a_0_7;
        act_no_action;
    }
    default_action: act_no_action;
}




/*********************  array a_1_0  *************************/
register a_1_0
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_0
{
    reg: a_1_0;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_0
{
    reg: a_1_0;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_0()
{
    b_insert_a_1_0.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_0()
{
    b_get_a_1_0.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 0: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_0
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_0;
        act_get_a_1_0;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_1  *************************/
register a_1_1
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_1
{
    reg: a_1_1;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_1
{
    reg: a_1_1;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_1()
{
    b_insert_a_1_1.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_1()
{
    b_get_a_1_1.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 1: do insert
    is_INT == 1 && row1_offset == 1: do get
*/
table tbl_a_1_1
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_1;
        act_get_a_1_1;
        act_no_action;
    }
    default_action: act_no_action;
}



/*********************  array a_1_2  *************************/
register a_1_2
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_2
{
    reg: a_1_2;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_2
{
    reg: a_1_2;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_2()
{
    b_insert_a_1_2.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_2()
{
    b_get_a_1_2.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 2: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_2
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_2;
        act_get_a_1_2;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_3  *************************/
register a_1_3
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_3
{
    reg: a_1_3;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_3
{
    reg: a_1_3;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_3()
{
    b_insert_a_1_3.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_3()
{
    b_get_a_1_3.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 3: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_3
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_3;
        act_get_a_1_3;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_4  *************************/
register a_1_4
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_4
{
    reg: a_1_4;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_4
{
    reg: a_1_4;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_4()
{
    b_insert_a_1_4.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_4()
{
    b_get_a_1_4.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 4: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_4
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_4;
        act_get_a_1_4;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_5  *************************/
register a_1_5
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_5
{
    reg: a_1_5;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_5
{
    reg: a_1_5;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_5()
{
    b_insert_a_1_5.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_5()
{
    b_get_a_1_5.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 5: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_5
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_5;
        act_get_a_1_5;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_6  *************************/
register a_1_6
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_6
{
    reg: a_1_6;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_6
{
    reg: a_1_6;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_6()
{
    b_insert_a_1_6.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_6()
{
    b_get_a_1_6.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 6: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_6
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_6;
        act_get_a_1_6;
        act_no_action;
    }
    default_action: act_no_action;
}

/*********************  array a_1_7  *************************/
register a_1_7
{
    width: 32;
    instance_count: ARRAY_COL_LEN;
}

blackbox stateful_alu b_insert_a_1_7
{
    reg: a_1_7;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_get_a_1_7
{
    reg: a_1_7;
    output_value: register_lo;
    output_dst: m.row1_value;
}

action act_insert_a_1_7()
{
    b_insert_a_1_7.execute_stateful_alu(m.row1_address);
}

action act_get_a_1_7()
{
    b_get_a_1_7.execute_stateful_alu(m.row1_address);
}

/*
    Only 2 entries:
    is_INT == 0 && row1_offset == 7: do insert
    is_INT == 1 && row1_offset == 0: do get
*/
table tbl_a_1_7
{
    reads
    {
        m.is_INT: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_insert_a_1_7;
        act_get_a_1_7;
        act_no_action;
    }
    default_action: act_no_action;
}



/**********************************     cookie_0_0               ****************************/
register cookie_0_0
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_0
{
    reg: cookie_0_0;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_0
{
    reg: cookie_0_0;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_0()
{
    b_update_cookie_0_0.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_0_set_satisfy()
{
    b_check_cookie_0_0.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 0);
}

/**
    only two entries: 
    m.is_INT == 0 && m.cookie_hit0 == 0 && m.row0_offset == 0 : update_cookie_0_0
    m.is_INT == 1 && m.cookie_hit0 == 0 && m.row0_offset == 0 : act_check_cookie_0_0_set_satisfy
    default: no action
*/

table tbl_cookie_0_0
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_0;
        act_check_cookie_0_0_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_0_1               ****************************/
register cookie_0_1
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_1
{
    reg: cookie_0_1;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_1
{
    reg: cookie_0_1;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_1()
{
    b_update_cookie_0_1.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_1_set_satisfy()
{
    b_check_cookie_0_1.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 1);
}

/**
    only two entries: 
    m.is_INT == 0 && m.cookie_hit0 == 0 && m.row0_offset == 1 : update_cookie_0_1
    m.is_INT == 1 && m.cookie_hit0 == 0 && m.row0_offset == 0 : act_check_cookie_0_1_set_satisfy
    default: no action
*/


table tbl_cookie_0_1
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_1;
        act_check_cookie_0_1_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}


/**********************************     cookie_0_2               ****************************/
register cookie_0_2
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_2
{
    reg: cookie_0_2;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_2
{
    reg: cookie_0_2;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_2()
{
    b_update_cookie_0_2.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_2_set_satisfy()
{
    b_check_cookie_0_2.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 2);
}


table tbl_cookie_0_2
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_2;
        act_check_cookie_0_2_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}


/**********************************     cookie_0_3               ****************************/
register cookie_0_3
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_3
{
    reg: cookie_0_3;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_3
{
    reg: cookie_0_3;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_3()
{
    b_update_cookie_0_3.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_3_set_satisfy()
{
    b_check_cookie_0_3.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 3);
}



table tbl_cookie_0_3
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_3;
        act_check_cookie_0_3_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_0_4               ****************************/
register cookie_0_4
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_4
{
    reg: cookie_0_4;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_4
{
    reg: cookie_0_4;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_4()
{
    b_update_cookie_0_4.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_4_set_satisfy()
{
    b_check_cookie_0_4.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 4);
}



table tbl_cookie_0_4
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_4;
        act_check_cookie_0_4_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_0_5               ****************************/
register cookie_0_5
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_5
{
    reg: cookie_0_5;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_5
{
    reg: cookie_0_5;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_5()
{
    b_update_cookie_0_5.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_5_set_satisfy()
{
    b_check_cookie_0_5.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 5);
}


table tbl_cookie_0_5
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_5;
        act_check_cookie_0_5_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_0_6               ****************************/
register cookie_0_6
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_6
{
    reg: cookie_0_6;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_6
{
    reg: cookie_0_6;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_6()
{
    b_update_cookie_0_6.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_6_set_satisfy()
{
    b_check_cookie_0_6.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 6);
}




table tbl_cookie_0_6
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_6;
        act_check_cookie_0_6_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_0_7               ****************************/
register cookie_0_7
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_0_7
{
    reg: cookie_0_7;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_0_7
{
    reg: cookie_0_7;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit0;
    
}

action act_update_cookie_0_7()
{
    b_update_cookie_0_7.execute_stateful_alu(m.row0_address);
}

action act_check_cookie_0_7_set_satisfy()
{
    b_check_cookie_0_7.execute_stateful_alu(m.row0_address);
    modify_field(m.row0_satisfy_cell_offset, 7);
}


table tbl_cookie_0_7
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit0: exact;
        m.row0_offset: exact;
    }
    actions
    {
        act_update_cookie_0_7;
        act_check_cookie_0_7_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_0               ****************************/
register cookie_1_0
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_0
{
    reg: cookie_1_0;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_0
{
    reg: cookie_1_0;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_0()
{
    b_update_cookie_1_0.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_0_set_satisfy()
{
    b_check_cookie_1_0.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 0);
}

/**
    only two entries: 
    m.is_INT == 0 && m.cookie_hit1 == 0 && m.row1_offset == 0 : update_cookie_1_0
    m.is_INT == 1 && m.cookie_hit1 == 0 && m.row1_offset == 0 : act_check_cookie_1_0_set_both
    default: no action
*/

table tbl_cookie_1_0
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_0;
        act_check_cookie_1_0_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_1               ****************************/
register cookie_1_1
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_1
{
    reg: cookie_1_1;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_1
{
    reg: cookie_1_1;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_1()
{
    b_update_cookie_1_1.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_1_set_satisfy()
{
    b_check_cookie_1_1.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 1);
}

/**
    only two entries: 
    m.is_INT == 0 && m.cookie_hit1 == 0 && m.row1_offset == 1 : update_cookie_1_1
    m.is_INT == 1 && m.cookie_hit1 == 0 && m.row1_offset == 0 : act_check_cookie_1_1_set_both
    default: no action
*/


table tbl_cookie_1_1
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_1;
        act_check_cookie_1_1_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_2               ****************************/
register cookie_1_2
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_2
{
    reg: cookie_1_2;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_2
{
    reg: cookie_1_2;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_2()
{
    b_update_cookie_1_2.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_2_set_satisfy()
{
    b_check_cookie_1_2.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 2);
}


table tbl_cookie_1_2
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_2;
        act_check_cookie_1_2_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}


/**********************************     cookie_1_3               ****************************/
register cookie_1_3
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_3
{
    reg: cookie_1_3;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_3
{
    reg: cookie_1_3;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_3()
{
    b_update_cookie_1_3.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_3_set_satisfy()
{
    b_check_cookie_1_3.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 3);
}


table tbl_cookie_1_3
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_3;
        act_check_cookie_1_3_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_4               ****************************/
register cookie_1_4
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_4
{
    reg: cookie_1_4;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_4
{
    reg: cookie_1_4;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_4()
{
    b_update_cookie_1_4.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_4_set_satisfy()
{
    b_check_cookie_1_4.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 4);
}

table tbl_cookie_1_4
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_4;
        act_check_cookie_1_4_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_5               ****************************/
register cookie_1_5
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_5
{
    reg: cookie_1_5;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_5
{
    reg: cookie_1_5;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_5()
{
    b_update_cookie_1_5.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_5_set_satisfy()
{
    b_check_cookie_1_5.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 5);
}


table tbl_cookie_1_5
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_5;
        act_check_cookie_1_5_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_6               ****************************/
register cookie_1_6
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_6
{
    reg: cookie_1_6;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_6
{
    reg: cookie_1_6;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_6()
{
    b_update_cookie_1_6.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_6_set_satisfy()
{
    b_check_cookie_1_6.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 6);
}

table tbl_cookie_1_6
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_6;
        act_check_cookie_1_6_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}

/**********************************     cookie_1_7               ****************************/
register cookie_1_7
{
    width: 8;
    instance_count: COOKIE_COL_LEN;
}

blackbox stateful_alu b_update_cookie_1_7
{
    reg: cookie_1_7;
    update_lo_1_value: register_lo + 1;
}

blackbox stateful_alu b_check_cookie_1_7
{
    reg: cookie_1_7;
    condition_lo: register_lo >= m.cookie_threshold;
    update_lo_1_predicate: condition_lo;
    update_lo_1_value: 0;
    output_predicate: condition_lo;
    output_value: 1;
    output_dst: m.cookie_hit1;
    
}

action act_update_cookie_1_7()
{
    b_update_cookie_1_7.execute_stateful_alu(m.row1_address);
}

action act_check_cookie_1_7_set_satisfy()
{
    b_check_cookie_1_7.execute_stateful_alu(m.row1_address);
    modify_field(m.row1_satisfy_cell_offset, 7);
}


table tbl_cookie_1_7
{
    reads
    {
        m.is_INT: exact;
        m.cookie_hit1: exact;
        m.row1_offset: exact;
    }
    actions
    {
        act_update_cookie_1_7;
        act_check_cookie_1_7_set_satisfy;
        act_no_action;
    }
    default_action: act_no_action;
}


action act_update_offset_to_satisfy_0()
{
    modify_field(m.row0_offset, m.row0_satisfy_cell_offset);
}

/*
    If m.cookie_hit0 == 1 and m.is_INT == 1, do act_update_offset_to_satisfied_0
*/
table tbl_update_offset_to_valid_0
{
    reads
    {
        m.cookie_hit0: exact;
        m.is_INT: exact;
    }
    actions
    {
        act_update_offset_to_satisfy_0;
        act_no_action;
    }
    default_action:act_no_action;
}


action act_update_offset_to_satisfy_1()
{
    modify_field(m.row1_offset, m.row1_satisfy_cell_offset);
}

/*
    If m.cookie_hit1 == 1 and m.is_INT == 1, do act_update_offset_to_satisfied_1
*/
table tbl_update_offset_to_valid_1
{
    reads
    {
        m.cookie_hit1: exact;
        m.is_INT: exact;
    }
    actions
    {
        act_update_offset_to_satisfy_1;
        act_no_action;
    }
    default_action:act_no_action;
}

control ingress
{
    if(valid(ipv4))
    {
        if(valid(udp))
        {
            //determine egress port 
            apply(tbl_forward);
            //determine update index(loc0, loc1)
            apply(tbl_getHashIndex1);
            apply(tbl_getHashIndex0);
            
            //get cookie's random access position
            apply(tbl_getRandValue);
        }
    }
    if(valid(Scatter_s))
    {
        apply(tbl_set_is_INT);
        apply(tbl_modify_address);
    }
    apply(tbl_get_dune_threshold);
    apply(tbl_cookie_0_0);
    apply(tbl_cookie_0_1);
    apply(tbl_cookie_0_2);
    apply(tbl_cookie_0_3);
    apply(tbl_cookie_0_4);
    apply(tbl_cookie_0_5);
    apply(tbl_cookie_0_6);
    apply(tbl_cookie_0_7);

    apply(tbl_cookie_1_0);
    apply(tbl_cookie_1_1);
    apply(tbl_cookie_1_2);
    apply(tbl_cookie_1_3);
    apply(tbl_cookie_1_4);
    apply(tbl_cookie_1_5);
    apply(tbl_cookie_1_6);
    apply(tbl_cookie_1_7);
}


control egress
{
    apply(tbl_update_offset_to_valid_0);
    apply(tbl_update_offset_to_valid_1);
    //update or get value from array
    apply(tbl_a_0_0);
    apply(tbl_a_1_0);
    apply(tbl_a_0_1);
    apply(tbl_a_0_2);
    apply(tbl_a_0_3);
    apply(tbl_a_0_4);
    apply(tbl_a_0_5);
    apply(tbl_a_0_6);
    apply(tbl_a_0_7);
    
   
    apply(tbl_a_1_1);
    apply(tbl_a_1_2);
    apply(tbl_a_1_3);
    apply(tbl_a_1_4);
    apply(tbl_a_1_5);
    apply(tbl_a_1_6);
    apply(tbl_a_1_7);
    if(m.is_INT == 1)
        apply(take_t);

}