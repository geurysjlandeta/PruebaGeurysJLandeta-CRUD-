using PruebaGeurysJLandeta_CRUD_.Models;
using PruebaGeurysJLandeta_CRUD_.Models.Data;
using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Web;
using System.Web.Mvc;

namespace PruebaGeurysJLandeta_CRUD_.Controllers
{
    public class CustomerTypeController : Controller
    {
        private readonly TestInvoceDbContext _dbContext = new TestInvoceDbContext();
        public ActionResult CustomerType()
        {
            var data = _dbContext.CustomerTypes.ToList();
            return View(data);
        }

        public ActionResult AddCustomerType()
        {
            CustomerType customerType = new CustomerType();
            return View(customerType);
        }
        [HttpPost]
        public ActionResult AddCustomerType(CustomerType customerType)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    var datos = _dbContext.CustomerTypes.Add(customerType);
                    _dbContext.SaveChanges();

                    return Redirect("/CustomerType/CustomerType");
                }

                return View(customerType);
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }
        public ActionResult EditCustomerType(int id)
        {
            var data = _dbContext.CustomerTypes.Find(id);
            CustomerType model = new CustomerType()
            {
                Id = data.Id,
                Description = data.Description
            };

            return View(model);
        }

        [HttpPost]
        public ActionResult EditCustomerType(CustomerType customerType)
        {
            try
            {
                if (ModelState.IsValid)
                {
                    var data = _dbContext.CustomerTypes.Find(customerType.Id);

                    data.Description = customerType.Description;

                    _dbContext.Entry(data).State = EntityState.Modified;
                    _dbContext.SaveChanges();

                    return Redirect("/CustomerType/CustomerType");
                }

                return View(customerType);
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }
        [HttpGet]
        public ActionResult DeleteCustomerType(CustomerType customerType)
        {
            try
            {
                var data = _dbContext.CustomerTypes.Find(customerType.Id);

                _dbContext.CustomerTypes.Remove(data);
                _dbContext.SaveChanges();

                return Redirect("/CustomerType/CustomerType");
            }
            catch (Exception ex)
            {
                throw new Exception(ex.Message);
            }
        }

    }
}