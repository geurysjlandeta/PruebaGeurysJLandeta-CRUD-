using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Web;

namespace PruebaGeurysJLandeta_CRUD_.Models
{
    [Table("Customers")]
    public class Customer
    {
        public int Id { get; set; }
        [Required]
        public string CustName { get; set; }
        public string Adress { get; set; }
        public bool Status { get; set; }
        public int CustomerTypeId { get; set; }
    }
}