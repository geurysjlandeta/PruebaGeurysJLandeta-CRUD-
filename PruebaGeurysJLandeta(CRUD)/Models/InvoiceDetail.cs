using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Web;

namespace PruebaGeurysJLandeta_CRUD_.Models
{
    [Table("InvoiceDetail")]
    public class InvoiceDetail
    {
        public int Id { get; set; }
        public int CustomerId { get; set; }
        public int Qty { get; set; }
        public double Price { get; set; }
        public double TotalItbis { get; set; }
        public double SubTotal { get; set; }
        public double Total { get; set; }
        public bool Status { get; set; }
    }
}