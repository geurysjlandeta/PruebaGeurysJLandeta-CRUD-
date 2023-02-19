using PruebaGeurysJLandeta_CRUD_.Models;
using PruebaGeurysJLandeta_CRUD_.Models.Data;
using System;
using System.Collections.Generic;
using System.Data.Entity.Core.Objects;
using System.Linq;
using System.Web;
using System.Web.Mvc;

namespace PruebaGeurysJLandeta_CRUD_.Controllers
{
    public class InvoiceController : Controller
    {
        private readonly TestInvoceDbContext _dbContext = new TestInvoceDbContext();
        public ActionResult Invoice()
        {
            var data = _dbContext.Invoices.ToList();
            return View(data);
        }


        public ActionResult InvoiceDetail()
        {
            List<InvoiceDetail> invoiceDetail = new List<InvoiceDetail>();
            return View(invoiceDetail);
        }

        public ActionResult GetInvoiceDetail()
        {
            var model = _dbContext.InvoiceDetails.Where(detail => detail.Status == false);
            return View(model);
        }
        public ActionResult AddInvoiceDetail()
        {
            InvoiceDetail invoiceDetail = new InvoiceDetail();
            return View(invoiceDetail);
        }
        [HttpPost]
        public ActionResult AddInvoiceDetail(InvoiceDetail invoiceDetail)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    //int invoiceId = CreateHeaderInvoice(invoiceDetail);
                    InvoiceDetail detail = new InvoiceDetail()
                    {
                        //Id = invoiceId,
                        CustomerId = invoiceDetail.CustomerId,
                        Price = invoiceDetail.Price,
                        Qty = invoiceDetail.Qty,
                        SubTotal = invoiceDetail.SubTotal,
                        TotalItbis = invoiceDetail.TotalItbis,
                        Total = invoiceDetail.Total,
                        Status = invoiceDetail.Status,
                    };
                    var data = _dbContext.InvoiceDetails.Add(invoiceDetail);
                    _dbContext.SaveChanges();

                    Redirect("Invoice/GetInvoiceDetail");
                }
                return View(invoiceDetail);
            }
            catch (Exception ex)
            {

                throw new Exception(ex.Message);
            }


           
        }

        private int CreateHeaderInvoice(InvoiceDetail invoiceDetail)
        {
            try
            {
                var invoice = new Invoice()
                {
                    CustomerId = invoiceDetail.CustomerId,
                };

                var data =  _dbContext.Invoices.Add(invoice);
                _dbContext.SaveChanges();

                return data.Id;
            }
            catch (Exception)
            {

                throw;
            }
        }
    }
}